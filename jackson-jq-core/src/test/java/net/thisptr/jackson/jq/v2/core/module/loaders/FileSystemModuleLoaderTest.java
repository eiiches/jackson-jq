package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.thisptr.jackson.jq.v2.core.ClassLoaderUtils;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileSystemModuleLoaderTest {
	/**
	 * Results are compared by jq value, not by JsonNode identity: the node class a literal
	 * compiles to is not what these tests are about.
	 */
	private static final Comparator<JsonNode> BY_JQ_VALUE = new JsonNodeComparator<>(Jackson2JsonProvider.getInstance());

	private Environment<JsonNode> env;

	@TempDir
	private @Nullable Path tempDir;

	@BeforeEach
	public void beforeEach() throws IOException {
		ModuleLoader<JsonNode> moduleLoader = setupModuleLoader(Objects.requireNonNull(tempDir));

		env = EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(moduleLoader)
				.build();
	}

	/**
	 * Copy modules from classpath to temporary directory.
	 * <p>
	 * This is required for native image because the module loader reads from an absolute directory,
	 * while native-image test resources are available through {@code resource:/} URIs.
	 * </p>
	 */
	private ModuleLoader<JsonNode> setupModuleLoader(Path tempDir) throws IOException {
		ClassLoaderUtils.copyResources(getClass().getClassLoader(), "classpath_modules", tempDir);
		return new FileSystemModuleLoader<>(Jackson2JsonProvider.getInstance(), tempDir);
	}

	@Test
	public void testSimple() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"simple\" as simple; simple::one");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(1)));
	}

	@Test
	public void testSiblingDefCallWithinModule() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"sibling_defs\" as m; m::exported_foo");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(11)));
	}

	@Test
	public void testRecursiveImports() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"recursive_imports/a\" as a; a::one");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).hasMessageContaining("imported recursively");
	}

	@Test
	public void testSearchPathOverrides() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"search_path_overrides/a\" as a; a::two");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(2)));
	}

	@Test
	public void testRepeatedPathComponents() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"repeated_path_components\" as a; a::one");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(1)));

		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr2 = env.compile("import \"repeated_path_components/repeated_path_components\" as a; a::one");
			expr2.apply(NullNode.getInstance(), value -> {
			});
		}).hasMessageContaining("must not have equal consecutive components");
	}

	@Test
	public void testDataImports() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"data_imports/a\" as $a; $a::a[]");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(1), IntNode.valueOf(2)));
	}

	@Test
	public void testBrokenDataImports() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"broken_data_imports/a\" as $a; $a::a");
			expr.apply(NullNode.getInstance(), value -> {
			});
		});
	}

	@Test
	public void testModuleNotFound() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"module_not_exist\" as a; a::one");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: module_not_exist")
				.extracting(e -> ((ModuleNotFoundException) e).getPath()).isEqualTo("module_not_exist");

		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"module_not_exist\" as $a; $a::a");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: module_not_exist")
				.extracting(e -> ((ModuleNotFoundException) e).getPath()).isEqualTo("module_not_exist");
	}

	@Test
	public void testIllegalSearchPathOverrides() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"illegal_search_path_overrides\" as a; a::one");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).hasMessageContaining("must stay within the original search path");
	}

	@Test
	public void testImportWithAbsolutePath() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"/foo\" as foo; foo::foo");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).hasMessageContaining("must be relative");
	}

	/**
	 * What the other loader in {@link #testModuleCanImportFromAnotherLoader} serves.
	 */
	private static final class OtherLoaderJqModule implements JqModule<JsonNode> {
		@Override
		public String getSource() {
			return "def two: 2;";
		}

		@Override
		public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			return o instanceof OtherLoaderJqModule;
		}

		@Override
		public int hashCode() {
			return OtherLoaderJqModule.class.hashCode();
		}
	}

	/**
	 * A module read off the search path can import one that a different loader serves: the compiler
	 * resolves a module's imports through every loader the environment has, not just the one the
	 * module came from. This is what the removed {@code parentModuleLoader} was reaching for.
	 */
	@Test
	public void testModuleCanImportFromAnotherLoader() throws Exception {
		Path dir = Objects.requireNonNull(tempDir);
		Files.write(dir.resolve("uses_other.jq"), "import \"other\" as other; def one: other::two - 1;".getBytes(StandardCharsets.UTF_8));

		Environment<JsonNode> mixedEnv = EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(new FileSystemModuleLoader<>(Jackson2JsonProvider.getInstance(), dir))
				.addModuleLoader(new ModuleLoader<JsonNode>() {
					@Override
					public Module loadModule(String path, Maybe<JsonNode> metadata) {
						if (!"other".equals(path))
							throw new ModuleNotFoundException(path);
						return new OtherLoaderJqModule();
					}

					@Override
					public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
						throw new ModuleNotFoundException(path);
					}
				})
				.build();

		JsonQuery<JsonNode> expr = mixedEnv.compile("import \"uses_other\" as a; a::one");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(1)));
	}

	/**
	 * An import path that resolves to the search path itself must be refused, whichever way it is
	 * spelled. It looks harmless, but the file this loader then looks for is a *sibling* of what it
	 * resolved -- and the sibling of the search path is outside it. Real jq refuses all three of
	 * these too.
	 */
	@Test
	public void testImportResolvingToTheSearchPathItselfIsRefused() throws Exception {
		Path outside = Objects.requireNonNull(tempDir);
		Path searchPath = outside.resolve("root");
		Files.createDirectories(searchPath);
		Files.write(searchPath.resolve("inside.jq"), "def inside: 1;".getBytes(StandardCharsets.UTF_8));
		// The sibling of the search path: reachable only by escaping it.
		Files.write(outside.resolve("root.jq"), "def secret: \"leaked\";".getBytes(StandardCharsets.UTF_8));

		Environment<JsonNode> rootEnv = EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(new FileSystemModuleLoader<>(Jackson2JsonProvider.getInstance(), searchPath))
				.build();

		for (String path : Arrays.asList(".", "", "inside/..", "../root")) {
			assertThatThrownBy(() -> rootEnv.compile("import \"" + path + "\" as m; m::secret"))
					.describedAs("import \"%s\"", path)
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("import path must");
		}

		// The same loader still resolves what it should.
		JsonQuery<JsonNode> expr = rootEnv.compile("import \"inside\" as m; m::inside");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(1)));
	}

	/**
	 * The same guard on the relative-import path: a {@code {search: ...}} override may land on the
	 * search root, and an import relative to it must not escape either.
	 */
	@Test
	public void testRelativeImportResolvingToTheSearchPathItselfIsRefused() throws Exception {
		Path outside = Objects.requireNonNull(tempDir);
		Path searchPath = outside.resolve("root2");
		Files.createDirectories(searchPath);
		Files.write(searchPath.resolve("a.jq"), "import \".\" as m {search: \"./\"}; def one: m::secret;".getBytes(StandardCharsets.UTF_8));
		Files.write(outside.resolve("root2.jq"), "def secret: \"leaked\";".getBytes(StandardCharsets.UTF_8));

		Environment<JsonNode> rootEnv = EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(new FileSystemModuleLoader<>(Jackson2JsonProvider.getInstance(), searchPath))
				.build();

		assertThatThrownBy(() -> rootEnv.compile("import \"a\" as a; a::one"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("import path must");
	}

	/**
	 * A symlink inside the search path is followed wherever it points, as jq does. Containment is
	 * about the paths this loader builds, not about what the filesystem does with them: a symlink in
	 * the tree is the operator's doing, and whoever could create one there could equally leave a
	 * {@code .jq} file there, which is executable code rather than a read. Pinned by a test because
	 * it is a decision, not an oversight.
	 */
	@Test
	public void testSymlinkInsideSearchPathIsFollowed() throws Exception {
		Path outside = Objects.requireNonNull(tempDir);
		Path searchPath = outside.resolve("root3");
		Files.createDirectories(searchPath);
		Files.write(outside.resolve("shared.jq"), "def shared: 42;".getBytes(StandardCharsets.UTF_8));
		try {
			Files.createSymbolicLink(searchPath.resolve("shared.jq"), outside.resolve("shared.jq"));
		} catch (UnsupportedOperationException | FileSystemException e) {
			Assumptions.abort("this filesystem does not support symlinks: " + e.getMessage());
		}

		Environment<JsonNode> linkedEnv = EnvironmentBuilder.<JsonNode>withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(new FileSystemModuleLoader<>(Jackson2JsonProvider.getInstance(), searchPath))
				.build();

		JsonQuery<JsonNode> expr = linkedEnv.compile("import \"shared\" as m; m::shared");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);
		assertThat(actual).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(IntNode.valueOf(42)));
	}

	@Test
	public void testDirectoryTraversal() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"../foo\" as foo; foo::foo");
			expr.apply(NullNode.getInstance(), value -> {
			});
		}).hasMessageContaining("must not traverse to parent directories");
	}
}
