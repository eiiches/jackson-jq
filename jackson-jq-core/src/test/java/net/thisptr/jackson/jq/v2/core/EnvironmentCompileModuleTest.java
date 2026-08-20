package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
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
		private final Environment<JsonNode> jsonProviderEnv;

		InMemoryModuleLoader(Environment<JsonNode> jsonProviderEnv) {
			this.jsonProviderEnv = jsonProviderEnv;
		}

		void put(String path, String source) {
			sources.put(path, source);
		}

		@Override
		public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
			String source = sources.get(path);
			if (source == null)
				return null;
			Environment<JsonNode> moduleEnv = new Environment<>(jsonProviderEnv.jsonProvider(), jsonProviderEnv.version());
			moduleEnv.setModuleLoader(this);
			return moduleEnv.compileModule(source);
		}

		@Override
		public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
			return null;
		}
	}

	@Test
	public void testCustomModuleLoaderUsingCompileModule() throws Exception {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);

		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader(env);
		moduleLoader.put("foo", "def bar: 42;");
		env.setModuleLoader(moduleLoader);

		JsonQuery<JsonNode> expr = env.compile("import \"foo\" as foo; foo::bar");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(42);
	}

	@Test
	public void testCompileModuleExposesAllTopLevelDefs() throws Exception {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);

		Module module = env.compileModule("def one: 1; def two: 2; def three($x): $x;");

		assertThat(module.getAllFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("one", 0),
				FunctionSignature.of("two", 0),
				FunctionSignature.of("three", 1));
	}
}
