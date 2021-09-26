package net.thisptr.jackson.jq.module.loaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.module.ModuleLoader;

public class FileSystemModuleLoaderTest {
	private Scope rootScope;

	@BeforeEach
	public void beforeEach() throws URISyntaxException {
		rootScope = Scope.newEmptyScope();

		final Path searchPath = Paths.get(FileSystemModuleLoaderTest.class.getClassLoader().getResource("classpath_modules").toURI());
		final ModuleLoader moduleLoader = new FileSystemModuleLoader(rootScope, Versions.JQ_1_6, searchPath);

		rootScope.setModuleLoader(moduleLoader);
	}

	@Test
	public void testSimple() throws Exception {
		final JsonQuery expr = JsonQuery.compile("import \"simple\" as simple; simple::one", Versions.JQ_1_6);
		final List<JsonNode> actual = new ArrayList<>();
		expr.apply(rootScope, NullNode.getInstance(), actual::add);
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1)));
	}

	@Test
	public void testRecursiveImports() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"recursive_imports/a\" as a; a::one", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("imported recursively");
	}

	@Test
	public void testSearchPathOverrides() throws Exception {
		final JsonQuery expr = JsonQuery.compile("import \"search_path_overrides/a\" as a; a::two", Versions.JQ_1_6);
		final List<JsonNode> actual = new ArrayList<>();
		expr.apply(rootScope, NullNode.getInstance(), actual::add);
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(2)));
	}

	@Test
	public void testRepeatedPathComponents() throws Exception {
		final JsonQuery expr = JsonQuery.compile("import \"repeated_path_components\" as a; a::one", Versions.JQ_1_6);
		final List<JsonNode> actual = new ArrayList<>();
		expr.apply(rootScope, NullNode.getInstance(), actual::add);
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1)));

		assertThatThrownBy(() -> {
			final JsonQuery expr2 = JsonQuery.compile("import \"repeated_path_components/repeated_path_components\" as a; a::one", Versions.JQ_1_6);
			expr2.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("must not have equal consecutive components");
	}

	@Test
	public void testDataImports() throws Exception {
		final JsonQuery expr = JsonQuery.compile("import \"data_imports/a\" as $a; $a::a[]", Versions.JQ_1_6);
		final List<JsonNode> actual = new ArrayList<>();
		expr.apply(rootScope, NullNode.getInstance(), actual::add);
		assertThat(actual).isEqualTo(Arrays.asList(IntNode.valueOf(1), IntNode.valueOf(2)));
	}

	@Test
	public void testBrokenDataImports() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"broken_data_imports/a\" as $a; $a::a", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		});
	}

	@Test
	public void testModuleNotFound() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"module_not_exist\" as a; a::one", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("module not found");

		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"module_not_exist\" as $a; $a::a", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("module not found");
	}

	@Test
	public void testIllegalSearchPathOverrides() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"illegal_search_path_overrides\" as a; a::one", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("must stay within the original search path");
	}

	@Test
	public void testImportWithAbsolutePath() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"/foo\" as foo; foo::foo", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("must be relative");
	}

	@Test
	public void testDirectoryTraversal() throws Exception {
		assertThatThrownBy(() -> {
			final JsonQuery expr = JsonQuery.compile("import \"../foo\" as foo; foo::foo", Versions.JQ_1_6);
			expr.apply(rootScope, NullNode.getInstance(), (value) -> {});
		}).hasMessageContaining("must be within the search path");
	}
}
