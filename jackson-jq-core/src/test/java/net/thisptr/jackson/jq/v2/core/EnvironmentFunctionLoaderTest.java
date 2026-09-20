package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// proves addFunctionLoader() actually takes effect on the built Environment.
public class EnvironmentFunctionLoaderTest {
	@Test
	public void classPathLoaderKeepsJavaAndJqDefinitionsSeparate() {
		ClassPathFunctionLoader loader = ClassPathFunctionLoader.getInstance();

		assertThat(loader.getFunctions(Versions.JQ_1_6))
				.containsKey(FunctionSignature.of("length", 0))
				.doesNotContainKey(FunctionSignature.of("map", 1));
		assertThat(loader.getJqFunctions(Versions.JQ_1_6))
				.containsKey(FunctionSignature.of("map", 1))
				.doesNotContainKey(FunctionSignature.of("length", 0));

		// isempty/1 is a Java function, registered from 1.6 on.
		assertThat(loader.getFunctions(Versions.JQ_1_6)).containsKey(FunctionSignature.of("isempty", 1));
		assertThat(loader.getJqFunctions(Versions.JQ_1_6)).doesNotContainKey(FunctionSignature.of("isempty", 1));
		assertThat(loader.getFunctions(Versions.JQ_1_5)).doesNotContainKey(FunctionSignature.of("isempty", 1));
	}

	private static Function constantFunction(String text) {
		return new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
				return (frame, in, path, output) -> output.emit(jsonProvider.createString(text), UntrackedPath.getInstance());
			}
		};
	}

	private static FunctionLoader constantLoader(FunctionSignature key, String text) {
		return functionLoader(Collections.singletonMap(key, constantFunction(text)), Collections.emptyMap());
	}

	private static FunctionLoader functionLoader(Map<FunctionSignature, Function> functions, Map<FunctionSignature, JqFunction> jqFunctions) {
		return new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return functions;
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return jqFunctions;
			}
		};
	}

	private static List<JsonNode> execute(Environment<JsonNode> env, String source) {
		JsonQuery<JsonNode> query = env.compile(source);
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);
		return out;
	}

	@Test
	public void addFunctionLoaderAfterConstructionTakesEffect() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(constantLoader(FunctionSignature.of("greet", 0), "hello"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("hello");
	}

	/**
	 * A loader is appended after the default {@link ClassPathFunctionLoader}, and the first loader to
	 * supply a name answers -- so an added loader extends the builtins, it does not shadow them.
	 */
	@Test
	public void addedFunctionLoaderDoesNotShadowABuiltin() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(constantLoader(FunctionSignature.of("not", 0), "overridden"))
				.build();

		assertThat(execute(env, "true | not")).extracting(JsonNode::asBoolean).containsExactly(false);
	}

	@Test
	public void clearFunctionLoadersLetsALoaderSupplyABuiltinName() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.clearFunctionLoaders()
				.addFunctionLoader(constantLoader(FunctionSignature.of("not", 0), "overridden"))
				.build();

		assertThat(execute(env, "true | not")).extracting(JsonNode::asText).containsExactly("overridden");
	}

	@Test
	public void explicitDefineFunctionWinsOverFunctionLoader() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(constantLoader(key, "from-loader"))
				.defineFunction(key, constantFunction("from-explicit"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("from-explicit");
	}

	@Test
	public void jqFunctionIsResolvedFromSeparateLoaderRegistry() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-jq");
	}

	/**
	 * The loader tier resolves exactly like the environment tier: an exact-signature Java function
	 * beats an exact-signature jq definition. Which language a function is written in is a detail of
	 * how it was supplied, so it must not decide the winner differently in one tier than in the other.
	 */
	@Test
	public void loadedJavaFunctionWinsOverLoaderJqFunctionWithSameExactSignature() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.singletonMap(key, constantFunction("from-java")), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-java");
	}

	/**
	 * The other half of that rule, and the half the Java-first ordering must not disturb: exact still
	 * beats variadic, so a loader jq definition wins over a variadic loader Java function.
	 */
	@Test
	public void exactLoaderJqFunctionWinsOverLoaderJavaVariadicFunction() {
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"exact-jq\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(
						Collections.singletonMap(FunctionSignature.ofVariadic("greet"), constantFunction("variadic-java")),
						Collections.singletonMap(FunctionSignature.of("greet", 0), jqFunction)))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("exact-jq");
	}

	@Test
	public void explicitFunctionWinsOverLoadedJqFunction() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.defineFunction(key, constantFunction("from-explicit"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-explicit");
	}

	@Test
	public void recursiveJqFunctionUsesGenericBodyAsRecursionGuard() {
		FunctionSignature key = FunctionSignature.of("countdown", 1);
		JqFunction jqFunction = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then 0 else countdown($n - 1) end");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "countdown(3)")).extracting(JsonNode::asInt).containsExactly(0);
	}

	@Test
	public void environmentJqFunctionUsesFullEnvironment() {
		JqFunction helper = JqFunction.of("jq_helper", Collections.emptyList(), "\"-jq\"");
		JqFunction greet = JqFunction.of("greet", Collections.emptyList(), "java_helper + jq_helper + $suffix");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineFunction(FunctionSignature.of("java_helper", 0), constantFunction("java"))
				.defineJqFunction(helper)
				.defineJqFunction(greet)
				.defineConstant("suffix", Jackson2JsonProvider.getInstance().createString("-constant"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("java-jq-constant");
	}

	@Test
	public void environmentJqFunctionIsRecursive() {
		JqFunction countdown = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then 0 else countdown($n - 1) end");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(countdown)
				.build();

		assertThat(execute(env, "countdown(3)")).extracting(JsonNode::asInt).containsExactly(0);
	}

	@Test
	public void recursiveEnvironmentJqFunctionCanUseDeclaredGlobals() {
		FunctionSignature helperSignature = FunctionSignature.of("declared_helper", 0);
		JqFunction countdown = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then declared_helper + $suffix else countdown($n - 1) end");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.declareFunction(helperSignature)
				.declareVariable("suffix")
				.defineJqFunction(countdown)
				.build();
		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setFunction(helperSignature, constantFunction("declared"))
				.setVariable("suffix", env.getJsonProvider().createString("-value"))
				.build();
		List<JsonNode> out = new ArrayList<>();

		env.compile("countdown(2)").withRuntimeBindings(bindings).apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).extracting(JsonNode::asText).containsExactly("declared-value");
	}

	@Test
	public void environmentJqFunctionWinsOverLoaderDefinition() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction loaded = JqFunction.of("greet", Collections.emptyList(), "\"from-loader\"");
		JqFunction defined = JqFunction.of("greet", Collections.emptyList(), "\"from-environment\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, loaded)))
				.defineJqFunction(defined)
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-environment");
	}

	@Test
	public void loaderJqFunctionCannotSeeEnvironmentDefinitions() {
		FunctionSignature key = FunctionSignature.of("loaded", 0);
		JqFunction loaded = JqFunction.of("loaded", Collections.emptyList(), "environment_only");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, loaded)))
				.defineFunction(FunctionSignature.of("environment_only", 0), constantFunction("visible"))
				.build();

		assertThatThrownBy(() -> execute(env, "loaded"))
				.hasMessageContaining("environment_only");
	}

	// JqFunctionCompiler's frame-elided ("inlined") call-site specialization shares the call site's
	// physical StackFrame with a leaf-shaped (no internal "def", non-recursive) library function's own
	// params/locals -- see CompileContext#pushInlinedFunctionScope. This must not let the library body
	// resolve a name it was never given as a param just because the calling query happens to have bound
	// one of the same name at that particular call site.
	@Test
	public void inlinedJqFunctionBodyCannotSeeTheCallSitesLocalVariable() {
		JqFunction leaks = JqFunction.of("leaks", Collections.singletonList(FunctionParameter.ofFilter("f")), "$outer");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(leaks)
				.build();

		assertThatThrownBy(() -> execute(env, "1 as $outer | leaks(.)"))
				.hasMessageContaining("outer");
	}

	// Same isolation property, but with the leaking reference nested one function boundary deeper inside
	// the library body (an internal def, the way recurse/until/while are shaped) -- exercises the
	// isolatesLexicalScope check still stopping the walk even after an ordinary function-boundary capture
	// hop has already been taken once.
	@Test
	public void inlinedJqFunctionBodyCannotSeeTheCallSitesLocalVariableThroughAnInternalDef() {
		JqFunction leaks = JqFunction.of("leaks", Collections.singletonList(FunctionParameter.ofFilter("f")), "def inner: $outer; inner");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(leaks)
				.build();

		assertThatThrownBy(() -> execute(env, "1 as $outer | leaks(.)"))
				.hasMessageContaining("outer");
	}

	// Repeated, sequential leaf-shaped library calls sharing one physical frame (map's own filter-param
	// slot reused across each `.[]` iteration) must not leak a previous iteration's bound argument into
	// the next one's.
	@Test
	public void repeatedInlinedCallsAtTheSameCallSiteDoNotAliasAcrossIterations() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6).build();

		assertThat(execute(env, "[1,2,3,4] | map(select(. % 2 == 0)) | add"))
				.extracting(JsonNode::asInt).containsExactly(6);
	}

	// A library function whose body contains an internal nested def (the recurse/until/while shape) is
	// deliberately excluded from the inlined fast path (see CompiledDefinition#eligibleForInlining) and
	// must keep working exactly as before through the unmodified compileResolvedFunction/bindResolved path.
	@Test
	public void jqFunctionWithAnInternalDefStillWorksThroughTheUnmodifiedPath() {
		JqFunction countUp = JqFunction.of("count_up", Collections.singletonList(FunctionParameter.ofValue("limit")),
				"def _step: if . >= $limit then . else (. + 1 | _step) end; _step");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(countUp)
				.build();
		List<JsonNode> out = new ArrayList<>();

		env.compile("count_up(5)").apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).extracting(JsonNode::asInt).containsExactly(5);
	}

	@Test
	public void exactEnvironmentJqFunctionWinsOverEnvironmentJavaVariadicFunction() {
		JqFunction defined = JqFunction.of("greet", Collections.emptyList(), "\"exact-jq\"");
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineFunction(FunctionSignature.ofVariadic("greet"), constantFunction("variadic-java"))
				.defineJqFunction(defined)
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("exact-jq");
	}

	@Test
	public void environmentJqFunctionMapIsAnImmutableSnapshot() {
		JqFunction first = JqFunction.of("first", Collections.emptyList(), "1");
		JqFunction second = JqFunction.of("second", Collections.emptyList(), "2");
		EnvironmentBuilder<JsonNode> builder = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(first);
		Environment<JsonNode> env = builder.build();
		builder.defineJqFunction(second);

		assertThat(env.getJqFunctions()).containsOnlyKeys(FunctionSignature.of("first", 0));
		assertThatThrownBy(() -> env.getJqFunctions().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void environmentJqFunctionCannotBeOverriddenByBindings() {
		FunctionSignature signature = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(JqFunction.of("greet", Collections.emptyList(), "\"fixed\""))
				.build();
		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setFunction(signature, constantFunction("override"))
				.build();

		assertThatThrownBy(() -> env.compile("greet").withRuntimeBindings(bindings))
				.hasMessageContaining("fixed value in the Environment");
	}

	@Test
	public void environmentFunctionRegistrationsRejectExactSignatureConflicts() {
		FunctionSignature signature = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "1");

		assertThatThrownBy(() -> EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineFunction(signature, constantFunction("java"))
				.defineJqFunction(jqFunction))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(jqFunction)
				.defineFunction(signature, constantFunction("java")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.declareFunction(signature)
				.defineJqFunction(jqFunction))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(jqFunction)
				.declareFunction(signature))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void environmentJqFunctionRejectsIncompatibleVersionRange() {
		JqFunction incompatible = JqFunction.of("future", Collections.emptyList(), "1", VersionRange.valueOf("[1.7, )"));
		JqFunction compatible = JqFunction.of("current", Collections.emptyList(), "1", VersionRange.valueOf("[1.6, 1.7)"));
		EnvironmentBuilder<JsonNode> builder = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6);

		assertThatThrownBy(() -> builder.defineJqFunction(incompatible))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not support jq");
		assertThat(builder.defineJqFunction(compatible).build().getJqFunctions())
				.containsKey(FunctionSignature.of("current", 0));
	}

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6);
	}

	/**
	 * Stands in for a real loader: what matters here is only how many end up in the environment, so
	 * this one supplies nothing.
	 */
	private static FunctionLoader emptyLoader() {
		return functionLoader(Collections.emptyMap(), Collections.emptyMap());
	}

	@Test
	public void testDefaultEnvironmentHasTheClassPathFunctionLoader() {
		assertThat(builder().build().getFunctionLoaders()).hasSize(1);
	}

	@Test
	public void testAddedLoadersFollowTheDefaultInOrder() {
		assertThat(builder().addFunctionLoader(emptyLoader()).addFunctionLoader(emptyLoader()).build().getFunctionLoaders())
				.hasSize(3);
	}

	@Test
	public void testClearFunctionLoadersTakesOverTheOrder() {
		assertThat(builder().clearFunctionLoaders().addFunctionLoader(emptyLoader()).addFunctionLoader(emptyLoader()).build().getFunctionLoaders())
				.hasSize(2);
	}

	@Test
	public void testFunctionLoadersAreNotModifiableThroughTheEnvironment() {
		List<FunctionLoader> loaders = builder().build().getFunctionLoaders();

		assertThatThrownBy(() -> loaders.add(emptyLoader())).isInstanceOf(UnsupportedOperationException.class);
	}

	/**
	 * The function-side counterpart of an environment with no module loaders: the builtins are the
	 * default loader's, so dropping it leaves nothing but what the environment itself defines.
	 */
	@Test
	public void testEnvironmentWithNoFunctionLoadersHasNoBuiltins() {
		Environment<JsonNode> env = builder().clearFunctionLoaders().build();

		assertThat(env.getFunctionLoaders()).isEmpty();
		assertThatThrownBy(() -> env.compile("length"))
				.hasMessageContaining("Function length/0 does not exist");
	}

	@Test
	public void earlierFunctionLoaderWinsOverALaterOneWithTheSameSignature() {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = builder()
				.addFunctionLoader(constantLoader(key, "from-first"))
				.addFunctionLoader(constantLoader(key, "from-second"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-first");
	}

	@Test
	public void aLaterFunctionLoaderIsConsultedForASignatureTheEarlierOneLacks() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(constantLoader(FunctionSignature.of("greet", 0), "hello"))
				.addFunctionLoader(constantLoader(FunctionSignature.of("farewell", 0), "bye"))
				.build();

		assertThat(execute(env, "[greet, farewell]")).flatExtracting(node -> List.of(node.get(0).asText(), node.get(1).asText()))
				.containsExactly("hello", "bye");
	}

	/**
	 * A loader is a tier of its own, asked whole before the next one is: the first loader that supplies
	 * the name at any of its three steps answers the call. So an earlier loader's variadic function
	 * beats a later loader's exact one -- the same "first loader to resolve wins" rule module loaders
	 * follow, deliberately preferred over letting exactness cross a loader boundary.
	 */
	@Test
	public void anEarlierLoadersVariadicFunctionWinsOverALaterLoadersExactFunction() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(constantLoader(FunctionSignature.ofVariadic("greet"), "variadic-first"))
				.addFunctionLoader(constantLoader(FunctionSignature.of("greet", 0), "exact-second"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("variadic-first");
	}

	/**
	 * A loader's jq definition compiles against a throwaway environment carrying the caller's loaders
	 * (JqFunctionCompiler#resolveEnvironment), so its body must reach a function only a *different*
	 * loader supplies -- not just the one it came from.
	 */
	@Test
	public void aLoaderJqFunctionBodyResolvesAgainstEveryFunctionLoader() {
		JqFunction greet = JqFunction.of("greet", Collections.emptyList(), "helper");
		Environment<JsonNode> env = builder()
				.addFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(FunctionSignature.of("greet", 0), greet)))
				.addFunctionLoader(constantLoader(FunctionSignature.of("helper", 0), "from-other-loader"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-other-loader");
	}

	/**
	 * The same propagation, through the other derived environment: a module's source compiles against
	 * ModuleResolver#moduleEnvironment, which must carry the whole list too.
	 */
	@Test
	public void aModuleBodyResolvesAgainstEveryFunctionLoader() {
		Environment<JsonNode> env = builder()
				.addFunctionLoader(constantLoader(FunctionSignature.of("helper", 0), "from-loader"))
				.addImportedModule("lib", new SourceModule("def greet: helper;"))
				.build();

		assertThat(execute(env, "lib::greet")).extracting(JsonNode::asText).containsExactly("from-loader");
	}

	/**
	 * Minimal jq-source module: {@code addImportedModule} takes either kind, and this one makes the
	 * compiler go through ModuleResolver to compile it.
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
}
