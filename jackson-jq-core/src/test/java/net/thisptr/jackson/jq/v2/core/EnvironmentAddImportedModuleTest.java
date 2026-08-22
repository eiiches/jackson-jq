package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that {@link EnvironmentBuilder#addImportedModule(String, Module)} makes {@code alias::func(...)}
 * calls resolvable without an explicit {@code import} statement in the query text.
 */
public class EnvironmentAddImportedModuleTest {

	private static final class InMemoryModuleLoader implements ModuleLoader<JsonNode> {
		private final Map<String, Module> modules = new HashMap<>();

		void put(String path, Module module) {
			modules.put(path, module);
		}

		@Override
		public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
			return modules.get(path);
		}

		@Override
		public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
			return null;
		}
	}

	@Test
	public void testImportedModuleUsableWithoutImportStatement() throws Exception {
		Environment<JsonNode> tempEnv = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();
		Module mathModule = tempEnv.compileModule("def square($x): $x * $x;");

		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.addImportedModule("math", mathModule)
				.build();

		JsonQuery<JsonNode> expr = env.compile("math::square(5)");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(25);
	}

	@Test
	public void testExplicitImportShadowsBuilderRegisteredModule() throws Exception {
		Environment<JsonNode> tempEnv = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();
		Module builderModule = tempEnv.compileModule("def bar: 1;");
		Module loaderModule = tempEnv.compileModule("def bar: 2;");

		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader();
		moduleLoader.put("foo", loaderModule);

		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setModuleLoader(moduleLoader)
				.addImportedModule("foo", builderModule)
				.build();

		JsonQuery<JsonNode> expr = env.compile("import \"foo\" as foo; foo::bar");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(2);
	}

	@Test
	public void testModuleQualifiedCallFallsBackToVariadicFunction() throws Exception {
		Function countArgs = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> fprovider, List<Expression<Context, N>> fargs, Version ver) {
				return (frame, in, path, output) -> output.emit(fprovider.createNumber(fargs.size()), null);
			}
		};
		Module variadicModule = new Module() {
			@Override
			public Map<FunctionSignature, Function> getFunctions() {
				return Collections.singletonMap(FunctionSignature.of("greet", null), countArgs);
			}
		};

		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.addImportedModule("m", variadicModule)
				.build();

		JsonQuery<JsonNode> expr = env.compile("m::greet(1; 2; 3)");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), (val, path) -> actual.add(val));

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(3);
	}

	@Test
	public void testUnregisteredModuleAliasStillFails() {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();

		assertThatThrownBy(() -> env.compile("bogus::bar"))
				.isInstanceOf(JsonQueryException.class);
	}
}
