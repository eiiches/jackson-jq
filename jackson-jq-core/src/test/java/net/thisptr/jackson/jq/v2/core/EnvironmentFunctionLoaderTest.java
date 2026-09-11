package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionParameter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// proves setFunctionLoader() actually takes effect on the built Environment.
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
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, Version version) {
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

	private static List<JsonNode> execute(Environment<JsonNode> env, String source) throws Exception {
		JsonQuery<JsonNode> query = env.compile(source);
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);
		return out;
	}

	@Test
	public void setFunctionLoaderAfterConstructionTakesEffect() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(FunctionSignature.of("greet", 0), "hello"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("hello");
	}

	@Test
	public void setFunctionLoaderOverridesBuiltin() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(FunctionSignature.of("not", 0), "overridden"))
				.build();

		JsonQuery<JsonNode> query = env.compile("true | not");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("overridden");
	}

	@Test
	public void explicitAddFunctionWinsOverFunctionLoader() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(constantLoader(key, "from-loader"))
				.defineFunction(key, constantFunction("from-explicit"))
				.build();

		JsonQuery<JsonNode> query = env.compile("greet");
		List<JsonNode> out = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("from-explicit");
	}

	@Test
	public void jqFunctionIsResolvedFromSeparateLoaderRegistry() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-jq");
	}

	@Test
	public void jqFunctionWinsOverLoadedJavaFunctionWithSameExactSignature() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.singletonMap(key, constantFunction("from-java")), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-jq");
	}

	@Test
	public void explicitFunctionWinsOverLoadedJqFunction() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "\"from-jq\"");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.defineFunction(key, constantFunction("from-explicit"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-explicit");
	}

	@Test
	public void recursiveJqFunctionUsesGenericBodyAsRecursionGuard() throws Exception {
		FunctionSignature key = FunctionSignature.of("countdown", 1);
		JqFunction jqFunction = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then 0 else countdown($n - 1) end");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, jqFunction)))
				.build();

		assertThat(execute(env, "countdown(3)")).extracting(JsonNode::asInt).containsExactly(0);
	}

	@Test
	public void environmentJqFunctionUsesFullEnvironment() throws Exception {
		JqFunction helper = JqFunction.of("jq_helper", Collections.emptyList(), "\"-jq\"");
		JqFunction greet = JqFunction.of("greet", Collections.emptyList(), "java_helper + jq_helper + $suffix");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineFunction(FunctionSignature.of("java_helper", 0), constantFunction("java"))
				.defineJqFunction(helper)
				.defineJqFunction(greet)
				.defineConstant("suffix", Jackson2JsonProviderImpl.getInstance().createString("-constant"))
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("java-jq-constant");
	}

	@Test
	public void environmentJqFunctionIsRecursive() throws Exception {
		JqFunction countdown = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then 0 else countdown($n - 1) end");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(countdown)
				.build();

		assertThat(execute(env, "countdown(3)")).extracting(JsonNode::asInt).containsExactly(0);
	}

	@Test
	public void recursiveEnvironmentJqFunctionCanUseDeclaredGlobals() throws Exception {
		FunctionSignature helperSignature = FunctionSignature.of("declared_helper", 0);
		JqFunction countdown = JqFunction.of("countdown", Collections.singletonList(FunctionParameter.ofValue("n")),
				"if $n <= 0 then declared_helper + $suffix else countdown($n - 1) end");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.declareFunction(helperSignature)
				.declareVariable("suffix")
				.defineJqFunction(countdown)
				.build();
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(helperSignature, constantFunction("declared"))
				.setVariable("suffix", env.getJsonProvider().createString("-value"))
				.build();
		List<JsonNode> out = new ArrayList<>();

		env.compile("countdown(2)").apply(env.getJsonProvider().createNull(), bindings, out::add);

		assertThat(out).extracting(JsonNode::asText).containsExactly("declared-value");
	}

	@Test
	public void environmentJqFunctionWinsOverLoaderDefinition() throws Exception {
		FunctionSignature key = FunctionSignature.of("greet", 0);
		JqFunction loaded = JqFunction.of("greet", Collections.emptyList(), "\"from-loader\"");
		JqFunction defined = JqFunction.of("greet", Collections.emptyList(), "\"from-environment\"");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, loaded)))
				.defineJqFunction(defined)
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("from-environment");
	}

	@Test
	public void loaderJqFunctionCannotSeeEnvironmentDefinitions() {
		FunctionSignature key = FunctionSignature.of("loaded", 0);
		JqFunction loaded = JqFunction.of("loaded", Collections.emptyList(), "environment_only");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setFunctionLoader(functionLoader(Collections.emptyMap(), Collections.singletonMap(key, loaded)))
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
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
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
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(leaks)
				.build();

		assertThatThrownBy(() -> execute(env, "1 as $outer | leaks(.)"))
				.hasMessageContaining("outer");
	}

	// Repeated, sequential leaf-shaped library calls sharing one physical frame (map's own filter-param
	// slot reused across each `.[]` iteration) must not leak a previous iteration's bound argument into
	// the next one's.
	@Test
	public void repeatedInlinedCallsAtTheSameCallSiteDoNotAliasAcrossIterations() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6).build();

		assertThat(execute(env, "[1,2,3,4] | map(select(. % 2 == 0)) | add"))
				.extracting(JsonNode::asInt).containsExactly(6);
	}

	// A library function whose body contains an internal nested def (the recurse/until/while shape) is
	// deliberately excluded from the inlined fast path (see CompiledDefinition#eligibleForInlining) and
	// must keep working exactly as before through the unmodified compileResolvedFunction/bindResolved path.
	@Test
	public void jqFunctionWithAnInternalDefStillWorksThroughTheUnmodifiedPath() throws Exception {
		JqFunction countUp = JqFunction.of("count_up", Collections.singletonList(FunctionParameter.ofValue("limit")),
				"def _step: if . >= $limit then . else (. + 1 | _step) end; _step");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(countUp)
				.build();
		List<JsonNode> out = new ArrayList<>();

		env.compile("count_up(5)").apply(env.getJsonProvider().createNull(), out::add);

		assertThat(out).extracting(JsonNode::asInt).containsExactly(5);
	}

	@Test
	public void exactEnvironmentJqFunctionWinsOverEnvironmentJavaVariadicFunction() throws Exception {
		JqFunction defined = JqFunction.of("greet", Collections.emptyList(), "\"exact-jq\"");
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineFunction(FunctionSignature.ofVariadic("greet"), constantFunction("variadic-java"))
				.defineJqFunction(defined)
				.build();

		assertThat(execute(env, "greet")).extracting(JsonNode::asText).containsExactly("exact-jq");
	}

	@Test
	public void environmentJqFunctionMapIsAnImmutableSnapshot() {
		JqFunction first = JqFunction.of("first", Collections.emptyList(), "1");
		JqFunction second = JqFunction.of("second", Collections.emptyList(), "2");
		EnvironmentBuilder<JsonNode> builder = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(first);
		Environment<JsonNode> env = builder.build();
		builder.defineJqFunction(second);

		assertThat(env.getJqFunctions()).containsOnlyKeys(FunctionSignature.of("first", 0));
		assertThatThrownBy(() -> env.getJqFunctions().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void environmentJqFunctionCannotBeOverriddenByBindings() throws Exception {
		FunctionSignature signature = FunctionSignature.of("greet", 0);
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(JqFunction.of("greet", Collections.emptyList(), "\"fixed\""))
				.build();
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(signature, constantFunction("override"))
				.build();

		assertThatThrownBy(() -> env.compile("greet").apply(env.getJsonProvider().createNull(), bindings, value -> {
		}))
				.hasMessageContaining("fixed value in the Environment");
	}

	@Test
	public void environmentFunctionRegistrationsRejectExactSignatureConflicts() {
		FunctionSignature signature = FunctionSignature.of("greet", 0);
		JqFunction jqFunction = JqFunction.of("greet", Collections.emptyList(), "1");

		assertThatThrownBy(() -> new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineFunction(signature, constantFunction("java"))
				.defineJqFunction(jqFunction))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(jqFunction)
				.defineFunction(signature, constantFunction("java")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.declareFunction(signature)
				.defineJqFunction(jqFunction))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineJqFunction(jqFunction)
				.declareFunction(signature))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void environmentJqFunctionRejectsIncompatibleVersionRange() {
		JqFunction incompatible = JqFunction.of("future", Collections.emptyList(), "1", VersionRange.valueOf("[1.7, )"));
		JqFunction compatible = JqFunction.of("current", Collections.emptyList(), "1", VersionRange.valueOf("[1.6, 1.7)"));
		EnvironmentBuilder<JsonNode> builder = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);

		assertThatThrownBy(() -> builder.defineJqFunction(incompatible))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not support jq");
		assertThat(builder.defineJqFunction(compatible).build().getJqFunctions())
				.containsKey(FunctionSignature.of("current", 0));
	}
}
