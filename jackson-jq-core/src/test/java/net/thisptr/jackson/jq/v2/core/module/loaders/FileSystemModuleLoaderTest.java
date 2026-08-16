package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.thisptr.jackson.jq.v2.core.ClassLoaderUtils;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileSystemModuleLoaderTest {
	private Scope<JsonNode> rootScope;
	private Environment<JsonNode> env;

	@TempDir
	private @Nullable Path tempDir;

	@BeforeEach
	public void beforeEach() throws IOException {
		rootScope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);

		ModuleLoader<JsonNode> moduleLoader = setupModuleLoader(Objects.requireNonNull(tempDir));

		env.setModuleLoader(moduleLoader);
	}

	/**
	 * Copy modules from classpath to temporary directory.
	 * <p>
	 * This is required for native image because the module loader reads from an absolute directory,
	 * while native-image test resources are available through {@code resource:/} URIs.
	 * </p>
	 */
	private ModuleLoader<JsonNode> setupModuleLoader(Path tempDir) throws IOException {
		ClassLoaderUtils.walk("classpath_modules", (src, relativePath) -> {
			try {
				Path dest = tempDir.resolve(relativePath.toString());
				if (Files.isDirectory(src)) {
					Files.createDirectories(dest);
				} else {
					Files.copy(src, dest);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		return new FileSystemModuleLoader<>(rootScope, Versions.JQ_1_6, tempDir);
	}

	@Test
	public void testSimple() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"simple\" as simple; simple::one");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1)));
	}

	@Test
	public void testRecursiveImports() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"recursive_imports/a\" as a; a::one");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("imported recursively");
	}

	@Test
	public void testSearchPathOverrides() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"search_path_overrides/a\" as a; a::two");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(2)));
	}

	@Test
	public void testRepeatedPathComponents() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"repeated_path_components\" as a; a::one");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1)));

		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr2 = env.compile("import \"repeated_path_components/repeated_path_components\" as a; a::one");
			expr2.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("must not have equal consecutive components");
	}

	@Test
	public void testDataImports() throws Exception {
		JsonQuery<JsonNode> expr = env.compile("import \"data_imports/a\" as $a; $a::a[]");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1), IntNode.valueOf(2)));
	}

	@Test
	public void testBrokenDataImports() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"broken_data_imports/a\" as $a; $a::a");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		});
	}

	@Test
	public void testModuleNotFound() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"module_not_exist\" as a; a::one");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("module not found");

		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"module_not_exist\" as $a; $a::a");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("module not found");
	}

	@Test
	public void testIllegalSearchPathOverrides() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"illegal_search_path_overrides\" as a; a::one");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("must stay within the original search path");
	}

	@Test
	public void testImportWithAbsolutePath() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"/foo\" as foo; foo::foo");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("must be relative");
	}

	@Test
	public void testDirectoryTraversal() throws Exception {
		assertThatThrownBy(() -> {
			JsonQuery<JsonNode> expr = env.compile("import \"../foo\" as foo; foo::foo");
			expr.apply(NullNode.getInstance(), (value, path) -> {});
		}).hasMessageContaining("must be within the search path");
	}
}
