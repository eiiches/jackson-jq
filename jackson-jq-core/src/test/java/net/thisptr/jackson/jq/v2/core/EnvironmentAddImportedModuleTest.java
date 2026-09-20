package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

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
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			Module module = modules.get(path);
			if (module == null)
				throw new ModuleNotFoundException(path);
			return module;
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}
	}

	/**
	 * A module added to an environment may be jq source rather than Java: the compiler compiles it
	 * the first time a query calls into it.
	 */
	private static final class SourceModule implements JqModule<JsonNode> {
		private final String source;

		SourceModule(String source) {
			this.source = source;
		}

		@Override
		public String getSourceCode() {
			return source;
		}

		@Override
		public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}
	}

	@Test
	public void testImportedModuleUsableWithoutImportStatement() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addImportedModule("math", new SourceModule("def square($x): $x * $x;"))
				.build();

		JsonQuery<JsonNode> expr = env.compile("math::square(5)");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(25);
	}

	@Test
	public void testExplicitImportShadowsBuilderRegisteredModule() {
		SourceModule builderModule = new SourceModule("def bar: 1;");
		SourceModule loaderModule = new SourceModule("def bar: 2;");

		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader();
		moduleLoader.put("foo", loaderModule);

		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearModuleLoaders()
				.addModuleLoader(moduleLoader)
				.addImportedModule("foo", builderModule)
				.build();

		JsonQuery<JsonNode> expr = env.compile("import \"foo\" as foo; foo::bar");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(2);
	}

	@Test
	public void testModuleQualifiedCallFallsBackToVariadicFunction() {
		Function countArgs = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> fargs) {
				JsonProvider<N> fprovider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(fprovider.createNumber(fargs.size()), UntrackedPath.getInstance());
			}
		};
		JavaModule variadicModule = new JavaModule() {
			@Override
			public Map<FunctionSignature, Function> getFunctions() {
				return Collections.singletonMap(FunctionSignature.ofVariadic("greet"), countArgs);
			}
		};

		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addImportedModule("m", variadicModule)
				.build();

		JsonQuery<JsonNode> expr = env.compile("m::greet(1; 2; 3)");
		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);

		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).asInt()).isEqualTo(3);
	}

	@Test
	public void testUnregisteredModuleAliasStillFails() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6).build();

		assertThatThrownBy(() -> env.compile("bogus::bar"))
				.isInstanceOf(JsonQueryException.class);
	}
}
