package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that {@link Environment#compileModule(String)} lets a {@link ModuleLoader} be implemented
 * using only public API -- no {@code net.thisptr.jackson.jq.v2.internal.*} or
 * {@code net.thisptr.jackson.jq.v2.core.internal.*} imports.
 */
public class EnvironmentCompileModuleTest {

	private static final class InMemoryModuleLoader implements ModuleLoader<JsonNode> {
		private final Map<String, String> sources = new HashMap<>();
		private final Map<String, JsonNode> datas = new HashMap<>();
		private final JsonProvider<JsonNode> jsonProvider;
		private final Version jqVersion;

		InMemoryModuleLoader(JsonProvider<JsonNode> jsonProvider, Version jqVersion) {
			this.jsonProvider = jsonProvider;
			this.jqVersion = jqVersion;
		}

		void put(String path, String source) {
			sources.put(path, source);
		}

		void putData(String path, JsonNode data) {
			datas.put(path, data);
		}

		@Override
		public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
			String source = sources.get(path);
			if (source == null)
				return null;
			Environment<JsonNode> moduleEnv = new EnvironmentBuilder<>(jsonProvider, jqVersion)
					.setModuleLoader(this)
					.build();
			return moduleEnv.compileModule(source);
		}

		@Override
		public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
			return datas.get(path);
		}
	}

	@Test
	public void testCustomModuleLoaderUsingCompileModule() throws Exception {
		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);
		moduleLoader.put("foo", "def bar: 42;");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setModuleLoader(moduleLoader)
				.build();

		JsonQuery<JsonNode> expr = env.compile("import \"foo\" as foo; foo::bar");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(42);
	}

	@Test
	public void testCompileModuleExposesAllTopLevelDefs() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();

		Module module = env.compileModule("def one: 1; def two: 2; def three($x): $x;");

		assertThat(module.getModuleMeta().getDefinitions()).containsExactlyInAnyOrder(
				FunctionSignature.of("one", 0),
				FunctionSignature.of("two", 0),
				FunctionSignature.of("three", 1));
	}

	@Test
	public void testCompileModuleExposesMetadata() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();

		Module module = env.compileModule("module { \"author\": \"Alice\", \"version\": 1 }; def one: 1;");

		Map<String, JsonNode> metadata = module.getModuleMeta().getMetadata(env.getJsonProvider());
		assertThat(metadata).containsKey("author");
		assertThat(Objects.requireNonNull(metadata.get("author")).asText()).isEqualTo("Alice");
		assertThat(metadata).containsKey("version");
		assertThat(Objects.requireNonNull(metadata.get("version")).asInt()).isEqualTo(1);
	}

	@Test
	public void testCompileModuleExposesDependencies() throws Exception {
		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);
		moduleLoader.put("foo/bar", "def bar: 1;");
		moduleLoader.put("helpers", "def helper: 1;");
		moduleLoader.putData("data/nums", Jackson2JsonProviderImpl.getInstance().createArray());
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setModuleLoader(moduleLoader)
				.build();

		Module module = env.compileModule("import \"foo/bar\" as bar; import \"data/nums\" as $nums { \"search\": \"./data\" }; include \"helpers\"; def test: 1;");

		List<net.thisptr.jackson.jq.v2.spi.module.ModuleMeta.Dependency> deps = module.getModuleMeta().getDependencies();
		assertThat(deps).hasSize(3);

		assertThat(deps.get(0).getRelpath()).isEqualTo("foo/bar");
		assertThat(deps.get(0).isData()).isFalse();
		assertThat(deps.get(0).getAlias()).isEqualTo("bar");
		assertThat(deps.get(0).getImportMetadata(env.getJsonProvider())).isEmpty();

		assertThat(deps.get(1).getRelpath()).isEqualTo("data/nums");
		assertThat(deps.get(1).isData()).isTrue();
		assertThat(deps.get(1).getAlias()).isEqualTo("nums");
		Map<String, JsonNode> dep1Meta = deps.get(1).getImportMetadata(env.getJsonProvider());
		assertThat(dep1Meta).containsKey("search");
		assertThat(Objects.requireNonNull(dep1Meta.get("search")).asText()).isEqualTo("./data");

		assertThat(deps.get(2).getRelpath()).isEqualTo("helpers");
		assertThat(deps.get(2).isData()).isFalse();
		assertThat(deps.get(2).getAlias()).isNull();
		assertThat(deps.get(2).getImportMetadata(env.getJsonProvider())).isEmpty();
	}
}
