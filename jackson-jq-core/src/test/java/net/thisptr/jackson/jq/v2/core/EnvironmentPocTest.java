package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnvironmentPocTest {
	private static boolean isConstantExpression(Expression<?, ?> expr) {
		ExpressionProperties properties = AnalyzedExpression.propertiesOf(expr);
		return !properties.dependsOnInput() && !properties.dependsOnExternalState() && !FreeVariables.dependsOnVariables(expr);
	}

	private abstract static class AbstractForwardingFunction implements Function {
		@Override
		public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
			return arguments.get(0);
		}
	}

	/**
	 * Results are compared by jq value, not by JsonNode identity: the node class a literal
	 * compiles to is not what these tests are about.
	 */
	private static final Comparator<JsonNode> BY_JQ_VALUE = new JsonNodeComparator<>(Jackson2JsonProvider.getInstance());

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> jsonProvider = Jackson2JsonProvider.getInstance();

	private static FunctionLoader javaFunctionLoader(FunctionSignature signature, Function function) {
		return new FunctionLoader() {
			@Override
			public Map<FunctionSignature, Function> getFunctions(Version version) {
				return Collections.singletonMap(signature, function);
			}

			@Override
			public Map<FunctionSignature, JqFunction> getJqFunctions(Version version) {
				return Collections.emptyMap();
			}
		};
	}

	@Test
	public void testAddFunctionAndExecute() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("examplefn", 1), new Function() {
					@Override
					public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
						JsonProvider<N> provider = bindCtx.getJsonProvider();
						return (scope, in, path, output) -> {
							String text = provider.getString(in);
							output.emit(provider.createString("hello:" + text), path);
						};
					}
				})
				.build();

		JsonQuery<JsonNode> q = env.compile("examplefn(.)");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("\"world\""), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asText()).isEqualTo("hello:world");
	}

	@Test
	public void testAddVariableAndExecute() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineVariable("var", () -> jsonProvider.createNumber(42))
				.build();

		JsonQuery<JsonNode> q = env.compile("$var");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("{}"), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asInt()).isEqualTo(42);
	}


	@Test
	public void testConstantExpressionPropagationThroughPipes() throws Exception {
		// A function that reads `.` (dependsOnInput) but is otherwise deterministic
		// (dependsOnExternalState=false) -- registered through a FunctionLoader (like a real
		// built-in) rather than EnvironmentBuilder.defineFunction(), since only that path is resolved
		// via ResolvedFunctionCall.
		Function increment = new Function() {
			@Override
			public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
				return new ExpressionProperties(Cardinality.ONE, true, false);
			}

			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> provider = bindCtx.getJsonProvider();
				return (scope, in, path, output) -> output.emit(provider.createNumber(Objects.requireNonNull(provider.getNumberAsLongExact(in)) + 1), UntrackedPath.getInstance());
			}
		};
		FunctionLoader testLoader = javaFunctionLoader(FunctionSignature.of("increment", 0), increment);

		List<Boolean> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				captured.add(isConstantExpression(args.get(0)));
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};

		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.addFunctionLoader(testLoader)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// Each call is bound once, after its argument has been finalized and optionally folded.
		List<Boolean> perCompilation = new ArrayList<>();
		for (String query : List.of("probe(1 | increment)", "probe(increment)", "probe(. | increment)", "probe(. + 1)", "probe(1 | . + 1)")) {
			env.compile(query);
			perCompilation.add(captured.get(captured.size() - 1));
		}

		assertThat(perCompilation).isEqualTo(List.of(true, false, false, false, true));
	}

	@Test
	public void testDependsOnExternalStateIsNotShielded() throws Exception {
		// A function that ignores `.` (dependsOnInput=false) but is non-deterministic
		// (dependsOnExternalState=true), like a real `random`/`now`.
		Function random = new Function() {
			@Override
			public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
				return new ExpressionProperties(Cardinality.ONE, false, true);
			}

			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				JsonProvider<N> provider = bindCtx.getJsonProvider();
				return (scope, in, path, output) -> output.emit(provider.createNumber(0), UntrackedPath.getInstance());
			}
		};
		FunctionLoader testLoader = javaFunctionLoader(FunctionSignature.of("random", 0), random);

		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};

		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.addFunctionLoader(testLoader)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("probe(1 | random)");
		Expression<?, JsonNode> onePipeRandom = captured.get(captured.size() - 1);
		assertThat(AnalyzedExpression.propertiesOf(onePipeRandom).dependsOnInput()).isFalse();
		assertThat(AnalyzedExpression.propertiesOf(onePipeRandom).dependsOnExternalState()).isTrue();
		assertThat(isConstantExpression(onePipeRandom)).isFalse();

		// Deliberately NOT shielded (see plan): random's result is discarded, but the pipe as a
		// whole still conservatively reports dependsOnExternalState()==true.
		env.compile("probe(random | 1)");
		Expression<?, JsonNode> randomPipeOne = captured.get(captured.size() - 1);
		assertThat(AnalyzedExpression.propertiesOf(randomPipeOne).dependsOnInput()).isFalse();
		assertThat(AnalyzedExpression.propertiesOf(randomPipeOne).dependsOnExternalState()).isTrue();
	}

	@Test
	public void testDependsOnVariablesClosesOverLocalBindings() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> withProbe = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineVariable("b", () -> jsonProvider.createNumber(1))
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// $b (a global variable) is always free -- non-const regardless of what it happens to hold.
		withProbe.compile("probe($b)");
		assertThat(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1))).isTrue();
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isFalse();

		// The binding is *inside* the expression -- $b is not free here, so the whole thing is const.
		withProbe.compile("probe(1 as $b | $b)");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isTrue();

		// `.` is still free even though the local $b binding is closed.
		withProbe.compile("probe(. as $b | $b)");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isFalse();
	}

	@Test
	public void testBuiltinFunctionCallDependsOnInputComposesFromBoundExpression() throws Exception {
		// error/1 analyze() correctly describes the complete call
		// (own contribution only when called with zero args). With a literal message and a non-fixed
		// `.`, reading the bound Expression's dependsOnInput() recognizes this as input-independent.
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// Top-level `.` is not fixed, so this only resolves as constant if error(null)'s
		// dependsOnInput() correctly reflects that its literal argument doesn't depend on input.
		env.compile("probe(error(null))");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isTrue();
		assertThat(captured.get(captured.size() - 1)).isInstanceOf(ConstantExpression.class);
	}

	@Test
	public void compilerPrecomputesEveryResultOfConstantFunctionArguments() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("probe((1, 2))");

		// The argument folds after type checking, so the binding that saw it folded is the last one.
		assertThat(captured.get(captured.size() - 1)).isInstanceOf(ConstantExpression.class);
		ConstantExpression<?, JsonNode> constant = (ConstantExpression<?, JsonNode>) captured.get(captured.size() - 1);
		assertThat(constant.getConstantResults()).usingElementComparator(BY_JQ_VALUE).isEqualTo(List.of(MAPPER.readTree("1"), MAPPER.readTree("2")));
	}

	@Test
	public void functionArgumentsUseTheirOwnInputDependencyScope() {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// Asserted per compilation, because a call whose argument folds is bound again afterwards.
		env.compile("1 | probe(\"literal\" | .)");
		assertThat(captured.get(captured.size() - 1)).isInstanceOf(ConstantExpression.class);

		env.compile("1 | probe(.)");
		assertThat(captured.get(captured.size() - 1)).isNotInstanceOf(ConstantExpression.class);
		assertThat(AnalyzedExpression.propertiesOf(captured.get(captured.size() - 1)).dependsOnInput()).isTrue();
	}

	@Test
	public void testLocalDefDependsOnFlagsComposeFromBody() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// A local def whose body is a literal, called with no arguments, is fully constant.
		env.compile("probe(def f: 1; f)");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isTrue();

		// A local def whose body reads `.` propagates dependsOnInput to its call sites.
		env.compile("probe(def f: .; f)");
		assertThat(AnalyzedExpression.propertiesOf(captured.get(captured.size() - 1)).dependsOnInput()).isTrue();

		// A one-hop capture ($x lives directly in the enclosing frame) is precisely subtracted by the
		// outer `as` binding, same as a plain variable read -- the whole thing folds to constant.
		env.compile("probe(1 as $x | def f: $x; f)");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isTrue();

		// A def that never references its own filter-typed parameter is still conservatively marked as
		// depending on input once called, because the argument expression is composed in regardless of
		// whether the body actually invokes it (mirrors testDependsOnExternalStateIsNotShielded's
		// "deliberately not shielded" precedent for builtin calls).
		env.compile("probe(def f(g): 1; f(. + 1))");
		assertThat(AnalyzedExpression.propertiesOf(captured.get(captured.size() - 1)).dependsOnInput()).isTrue();

		// A def whose body reads a *declared/global* variable stays conservative too -- that dependency
		// never touches CompileContext's closureSpec machinery at all (globals bypass the local/captured
		// scope-stack lookup entirely), so it can only be caught by also consulting the body's own
		// free-variable metadata directly (see Compiler.java's FunctionDefinitionAstNode branch).
		Environment<JsonNode> withGlobal = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineVariable("g", () -> jsonProvider.createNumber(1))
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();
		withGlobal.compile("probe(def f: $g; f)");
		assertThat(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1))).isTrue();
	}

	@Test
	public void testLocalDefCapturePrecisionThreadsThroughNestedDefs() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new AbstractForwardingFunction() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// $x is captured by `outer` (one hop, off the root frame), then captured *again* by `inner` --
		// reached through `outer`'s own closure. Resolving inner's reference threads a matching capture
		// entry through outer's own closureSpec too (see CompileContext#getVariableLocation), so calling
		// `outer` is still precisely known to depend on exactly $x's root-frame slot, and the enclosing
		// `as` binding correctly closes over it -- constant, even through the nested def.
		env.compile("probe(1 as $x | def outer: def inner: $x; inner; outer)");
		assertThat(isConstantExpression(captured.get(captured.size() - 1))).isTrue();

		// A *captured* call (reaching `outer` itself through a closure hop, from inside another def) does
		// stay conservatively opaque, though -- matching ResolvedCapturedVariableAccess's "defs stay
		// conservative" precedent for the call node itself, even though the callee's own info is precise.
		env.compile("probe(1 as $x | def outer: def inner: $x; inner; def middle: outer; middle)");
		assertThat(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1))).isTrue();
	}

	@Test
	public void testSelfRecursiveLocalDefCompilesAndStaysConservative() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		// The recursive call to `f` inside its own body has no FunctionDependsOnInfo available yet (it's
		// still being compiled), so it correctly falls back to the conservative default rather than
		// crashing or silently under-approximating.
		JsonQuery<JsonNode> q = env.compile("def f: if . == 0 then 0 else (. - 1 | f) end; f");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("3"), out::add);
		assertThat(out).hasSize(1);
		assertThat(out.get(0).asInt()).isEqualTo(0);
	}

	@Test
	public void testUndefinedFunctionThrowsAtCompileTime() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		assertThatThrownBy(() -> {
			env.compile("nonExistentFunc(.)");
		}).isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("nonExistentFunc/1 does not exist");
	}

	@Test
	public void testUndefinedVariableThrowsAtCompileTime() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		assertThatThrownBy(() -> {
			env.compile("$undefinedVar");
		}).isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Variable $undefinedVar is not defined");
	}

	@Test
	public void testLocalDefDoesNotLeakIntoGlobalFunctionTable() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		// First compile: defines and immediately uses a local `foo` -- must work.
		JsonQuery<JsonNode> q1 = env.compile("def foo: 1; foo");
		List<JsonNode> out = new ArrayList<>();
		q1.apply(MAPPER.readTree("null"), out::add);
		assertThat(out).hasSize(1);
		assertThat(out.get(0).asInt()).isEqualTo(1);

		// Second, independent compile on the SAME Environment: `foo` was never re-defined here, and must
		// not have been globally registered as a side effect of the first compile.
		assertThatThrownBy(() -> {
			env.compile("foo");
		}).isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("foo/0 does not exist");
	}

	@Test
	public void testLocalDefWithCaptureDoesNotLeakEitherAndFailsCleanlyAfterwards() {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		env.compile("1 as $x | def bar: $x; bar");

		assertThatThrownBy(() -> {
			env.compile("bar");
		}).isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("bar/0 does not exist");
	}

	@Test
	public void testLocalAstVariableResolution() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_7).build();

		JsonQuery<JsonNode> q = env.compile(". as $x | $x");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("123"), out::add);

		assertThat(out).hasSize(1);
		assertThat(out.get(0).asInt()).isEqualTo(123);
	}
}
