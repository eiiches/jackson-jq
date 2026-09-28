package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
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
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that {@link EnvironmentBuilder#includeModule(Module)} makes a module's functions callable by
 * their bare names, as a query's own {@code include} directive would, and pins where that sits in the
 * unqualified resolution order described by {@code docs/resolution-order.md}.
 */
public class EnvironmentIncludeModuleTest {

	private static final JsonProvider<JsonNode> PROVIDER = Jackson2JsonProvider.getInstance();

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
	 * A module an environment includes may be jq source rather than Java, in which case the compiler
	 * compiles it before it lowers the query that will call into it.
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


	}

	/**
	 * A Java {@link Function} of any arity that emits {@code value} and ignores its input.
	 */
	private static Function constant(String value) {
		return new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> fargs) {
				JsonProvider<N> provider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(provider.createString(value), UntrackedPath.getInstance());
			}
		};
	}

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(PROVIDER, Versions.JQ_1_6);
	}

	private static List<JsonNode> run(Environment<JsonNode> env, String query) {
		List<JsonNode> actual = new ArrayList<>();
		env.compile(query).apply(NullNode.getInstance(), actual::add);
		return actual;
	}

	@Test
	public void testIncludedJqModuleIsCallableWithoutAQualifier() {
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def square($x): $x * $x;"))
				.build();

		assertThat(run(env, "square(5)")).extracting(JsonNode::asInt).containsExactly(25);
	}

	@Test
	public void testIncludedModuleIsCallableFromInsideAQueryDefinition() {
		// The prelude case: a query's own `def` bodies resolve against the environment's includes too.
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def square($x): $x * $x;"))
				.build();

		assertThat(run(env, "def hypot($a; $b): square($a) + square($b); hypot(3; 4)"))
				.extracting(JsonNode::asInt).containsExactly(25);
	}

	@Test
	public void testIncludedJavaModuleIsCallableWithoutAQualifier() {
		JavaModule module = () -> Collections.singletonMap(FunctionSignature.of("greet", 0), constant("hi"));

		Environment<JsonNode> env = builder().includeModule(module).build();

		assertThat(run(env, "greet")).extracting(JsonNode::asText).containsExactly("hi");
	}

	@Test
	public void testUnqualifiedCallFallsBackToVariadicFunction() {
		Function countArgs = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> fargs) {
				JsonProvider<N> provider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(provider.createNumber(fargs.size()), UntrackedPath.getInstance());
			}
		};
		JavaModule module = () -> Collections.singletonMap(FunctionSignature.ofVariadic("greet"), countArgs);

		Environment<JsonNode> env = builder().includeModule(module).build();

		assertThat(run(env, "greet(1; 2; 3)")).extracting(JsonNode::asInt).containsExactly(3);
	}

	@Test
	public void testLocalDefinitionShadowsAnIncludedModule() {
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def greet: \"from-environment\";"))
				.build();

		assertThat(run(env, "def greet: \"from-query\"; greet")).extracting(JsonNode::asText).containsExactly("from-query");
	}

	@Test
	public void testQueryIncludeShadowsAnIncludedModule() {
		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader();
		moduleLoader.put("lib", new SourceModule("def greet: \"from-query-include\";"));

		Environment<JsonNode> env = builder()
				.clearModuleLoaders()
				.addModuleLoader(moduleLoader)
				.includeModule(new SourceModule("def greet: \"from-environment\";"))
				.build();

		assertThat(run(env, "include \"lib\"; greet")).extracting(JsonNode::asText).containsExactly("from-query-include");
	}

	@Test
	public void testEnvironmentDefinitionShadowsAnIncludedModule() {
		Environment<JsonNode> env = builder()
				.defineFunction(FunctionSignature.of("greet", 0), constant("from-defineFunction"))
				.includeModule(new SourceModule("def greet: \"from-environment-include\";"))
				.build();

		assertThat(run(env, "greet")).extracting(JsonNode::asText).containsExactly("from-defineFunction");
	}

	@Test
	public void testEnvironmentDeclarationShadowsAnIncludedModule() {
		// A declaration is a promise that every apply() will be handed an implementation. An included
		// module must not quietly stand in for the missing binding.
		Environment<JsonNode> env = builder()
				.declareFunction(FunctionSignature.of("greet", 0))
				.includeModule(new SourceModule("def greet: \"from-environment-include\";"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");

		assertThatThrownBy(() -> query.apply(NullNode.getInstance(), node -> {
		}))
				.isInstanceOf(JsonQueryException.class);

		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setFunction(FunctionSignature.of("greet", 0), constant("from-runtime-bindings"))
				.build();
		List<JsonNode> actual = new ArrayList<>();
		query.withRuntimeBindings(bindings).apply(NullNode.getInstance(), actual::add);

		assertThat(actual).containsExactly(TextNode.valueOf("from-runtime-bindings"));
	}

	@Test
	public void testAnIncludedModuleShadowsABuiltin() {
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def tojson: \"from-environment-include\";"))
				.build();

		assertThat(run(env, "tojson")).extracting(JsonNode::asText).containsExactly("from-environment-include");
	}

	@Test
	public void testLaterIncludedModuleShadowsAnEarlierOne() {
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def greet: \"first\";"))
				.includeModule(new SourceModule("def greet: \"second\";"))
				.build();

		assertThat(run(env, "greet")).extracting(JsonNode::asText).containsExactly("second");
	}

	@Test
	public void testASignatureTheIncludedModuleDoesNotExportStillResolves() {
		Environment<JsonNode> env = builder()
				.includeModule(new SourceModule("def greet: \"hi\";"))
				.build();

		assertThat(run(env, "[greet, (1 | tostring)]")).extracting(JsonNode::toString).containsExactly("[\"hi\",\"1\"]");
	}

	@Test
	public void testAnIncludedModuleIsNotVisibleInsideAnImportedModule() {
		// A library resolves its own names against its own imports, not against whatever its caller's
		// environment happens to include -- the same isolation the environment's globals get.
		InMemoryModuleLoader moduleLoader = new InMemoryModuleLoader();
		moduleLoader.put("lib", new SourceModule("def greet: helper;"));

		Environment<JsonNode> env = builder()
				.clearModuleLoaders()
				.addModuleLoader(moduleLoader)
				.includeModule(new SourceModule("def helper: 7;"))
				.build();

		assertThatThrownBy(() -> env.compile("import \"lib\" as lib; lib::greet"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("helper");
	}
}
