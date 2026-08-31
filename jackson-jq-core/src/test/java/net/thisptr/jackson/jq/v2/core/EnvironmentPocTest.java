package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExpressionUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnvironmentPocTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> jsonProvider = Jackson2JsonProviderImpl.getInstance();

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
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("examplefn", 1), new Function() {
					@Override
					public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version version) {
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

		assertEquals(1, out.size());
		assertEquals("hello:world", out.get(0).asText());
	}

	@Test
	public void testAddVariableAndExecute() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineVariable("var", () -> jsonProvider.createNumber(42))
				.build();

		JsonQuery<JsonNode> q = env.compile("$var");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("{}"), out::add);

		assertEquals(1, out.size());
		assertEquals(42, out.get(0).asInt());
	}


	@Test
	public void testConstantExpressionPropagationThroughPipes() throws Exception {
		// A function that reads `.` (dependsOnInput) but is otherwise deterministic
		// (dependsOnExternalState=false) -- registered through a FunctionLoader (like a real
		// built-in) rather than EnvironmentBuilder.defineFunction(), since only that path is resolved
		// via ResolvedFunctionCall.
		// bindArguments() uses FunctionBody, matching how every real Function now builds its bound
		// Expression -- Compiler.java reads these flags off the bound Expression.
		Function increment = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				return FunctionBody.<Context, N>builder(args).usesInput(true).build((scope, in, path, output) -> output.emit(provider.createNumber(Objects.requireNonNull(provider.getNumberAsLongExact(in)) + 1), UntrackedPath.getInstance()));
			}
		};
		FunctionLoader testLoader = javaFunctionLoader(FunctionSignature.of("increment", 0), increment);

		List<Boolean> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				captured.add(ExpressionUtils.isConstantExpression(args.get(0)));
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};

		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.setFunctionLoader(testLoader)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("probe(1 | increment)");
		env.compile("probe(increment)");
		env.compile("probe(. | increment)");
		env.compile("probe(. + 1)");
		env.compile("probe(1 | . + 1)");

		assertEquals(Arrays.asList(true, false, false, false, true), captured);
	}

	@Test
	public void testDependsOnExternalStateIsNotShielded() throws Exception {
		// A function that ignores `.` (dependsOnInput=false) but is non-deterministic
		// (dependsOnExternalState=true), like a real `random`/`now`.
		Function random = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				return FunctionBody.<Context, N>builder(args).usesExternalState(true).build((scope, in, path, output) -> output.emit(provider.createNumber(0), UntrackedPath.getInstance()));
			}
		};
		FunctionLoader testLoader = javaFunctionLoader(FunctionSignature.of("random", 0), random);

		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};

		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.setFunctionLoader(testLoader)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("probe(1 | random)");
		Expression<?, JsonNode> onePipeRandom = captured.get(captured.size() - 1);
		assertFalse(onePipeRandom.dependsOnInput());
		assertTrue(onePipeRandom.dependsOnExternalState());
		assertFalse(ExpressionUtils.isConstantExpression(onePipeRandom));

		// Deliberately NOT shielded (see plan): random's result is discarded, but the pipe as a
		// whole still conservatively reports dependsOnExternalState()==true.
		env.compile("probe(random | 1)");
		Expression<?, JsonNode> randomPipeOne = captured.get(captured.size() - 1);
		assertFalse(randomPipeOne.dependsOnInput());
		assertTrue(randomPipeOne.dependsOnExternalState());
	}

	@Test
	public void testDependsOnVariablesClosesOverLocalBindings() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> withProbe = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineVariable("b", () -> jsonProvider.createNumber(1))
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// $b (a global variable) is always free -- non-const regardless of what it happens to hold.
		withProbe.compile("probe($b)");
		assertTrue(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1)));
		assertFalse(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));

		// The binding is *inside* the expression -- $b is not free here, so the whole thing is const.
		withProbe.compile("probe(1 as $b | $b)");
		assertTrue(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));

		// `.` is still free even though the local $b binding is closed.
		withProbe.compile("probe(. as $b | $b)");
		assertFalse(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));
	}

	@Test
	public void testBuiltinFunctionCallDependsOnInputComposesFromBoundExpression() throws Exception {
		// error/1 bindArguments() correctly composes a precise bound Expression via FunctionBody
		// (own contribution only when called with zero args). With a literal message and a non-fixed
		// `.`, reading the bound Expression's dependsOnInput() recognizes this as input-independent.
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// Top-level `.` is not fixed, so this only resolves as constant if error(null)'s
		// dependsOnInput() correctly reflects that its literal argument doesn't depend on input.
		env.compile("probe(error(null))");
		assertTrue(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));
		assertTrue(captured.get(captured.size() - 1) instanceof ConstantExpression<?, ?>);
	}

	@Test
	public void compilerPrecomputesEveryResultOfConstantFunctionArguments() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("probe((1, 2))");

		assertTrue(captured.get(0) instanceof ConstantExpression<?, ?>);
		@SuppressWarnings("unchecked")
		ConstantExpression<?, JsonNode> constant = (ConstantExpression<?, JsonNode>) captured.get(0);
		assertEquals(Arrays.asList(MAPPER.readTree("1"), MAPPER.readTree("2")), constant.getConstantResults());
	}

	@Test
	public void functionArgumentsUseTheirOwnInputDependencyScope() {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		env.compile("1 | probe(\"literal\" | .)");
		env.compile("1 | probe(.)");

		assertTrue(captured.get(0) instanceof ConstantExpression<?, ?>);
		assertFalse(captured.get(1) instanceof ConstantExpression<?, ?>);
		assertTrue(captured.get(1).dependsOnInput());
	}

	@Test
	public void testLocalDefDependsOnFlagsComposeFromBody() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// A local def whose body is a literal, called with no arguments, is fully constant.
		env.compile("probe(def f: 1; f)");
		assertTrue(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));

		// A local def whose body reads `.` propagates dependsOnInput to its call sites.
		env.compile("probe(def f: .; f)");
		assertTrue(captured.get(captured.size() - 1).dependsOnInput());

		// A one-hop capture ($x lives directly in the enclosing frame) is precisely subtracted by the
		// outer `as` binding, same as a plain variable read -- the whole thing folds to constant.
		env.compile("probe(1 as $x | def f: $x; f)");
		assertTrue(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));

		// A def that never references its own filter-typed parameter is still conservatively marked as
		// depending on input once called, because the argument expression is composed in regardless of
		// whether the body actually invokes it (mirrors testDependsOnExternalStateIsNotShielded's
		// "deliberately not shielded" precedent for builtin calls).
		env.compile("probe(def f(g): 1; f(. + 1))");
		assertTrue(captured.get(captured.size() - 1).dependsOnInput());

		// A def whose body reads a *declared/global* variable stays conservative too -- that dependency
		// never touches CompileContext's closureSpec machinery at all (globals bypass the local/captured
		// scope-stack lookup entirely), so it can only be caught by also consulting the body's own
		// free-variable metadata directly (see Compiler.java's FunctionDefinitionAstNode branch).
		Environment<JsonNode> withGlobal = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineVariable("g", () -> jsonProvider.createNumber(1))
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();
		withGlobal.compile("probe(def f: $g; f)");
		assertTrue(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1)));
	}

	@Test
	public void testLocalDefCapturePrecisionThreadsThroughNestedDefs() throws Exception {
		List<Expression<?, JsonNode>> captured = new ArrayList<>();
		Function probe = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> provider, List<Expression<Context, N>> args, Version ver) {
				@SuppressWarnings("unchecked")
				Expression<?, JsonNode> arg = (Expression<?, JsonNode>) args.get(0);
				captured.add(arg);
				return (scope, in, path, output) -> output.emit(in, path);
			}
		};
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7)
				.defineFunction(FunctionSignature.of("probe", 1), probe)
				.build();

		// $x is captured by `outer` (one hop, off the root frame), then captured *again* by `inner` --
		// reached through `outer`'s own closure. Resolving inner's reference threads a matching capture
		// entry through outer's own closureSpec too (see CompileContext#getVariableLocation), so calling
		// `outer` is still precisely known to depend on exactly $x's root-frame slot, and the enclosing
		// `as` binding correctly closes over it -- constant, even through the nested def.
		env.compile("probe(1 as $x | def outer: def inner: $x; inner; outer)");
		assertTrue(ExpressionUtils.isConstantExpression(captured.get(captured.size() - 1)));

		// A *captured* call (reaching `outer` itself through a closure hop, from inside another def) does
		// stay conservatively opaque, though -- matching ResolvedCapturedVariableAccess's "defs stay
		// conservative" precedent for the call node itself, even though the callee's own info is precise.
		env.compile("probe(1 as $x | def outer: def inner: $x; inner; def middle: outer; middle)");
		assertTrue(FreeVariables.dependsOnVariables(captured.get(captured.size() - 1)));
	}

	@Test
	public void testSelfRecursiveLocalDefCompilesAndStaysConservative() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		// The recursive call to `f` inside its own body has no FunctionDependsOnInfo available yet (it's
		// still being compiled), so it correctly falls back to the conservative default rather than
		// crashing or silently under-approximating.
		JsonQuery<JsonNode> q = env.compile("def f: if . == 0 then 0 else (. - 1 | f) end; f");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("3"), out::add);
		assertEquals(1, out.size());
		assertEquals(0, out.get(0).asInt());
	}

	@Test
	public void testUndefinedFunctionThrowsAtCompileTime() {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("nonExistentFunc(.)");
		});

		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("nonExistentFunc/1 does not exist"));
	}

	@Test
	public void testUndefinedVariableThrowsAtCompileTime() {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("$undefinedVar");
		});

		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("Variable $undefinedVar is not defined"));
	}

	@Test
	public void testLocalDefDoesNotLeakIntoGlobalFunctionTable() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		// First compile: defines and immediately uses a local `foo` -- must work.
		JsonQuery<JsonNode> q1 = env.compile("def foo: 1; foo");
		List<JsonNode> out = new ArrayList<>();
		q1.apply(MAPPER.readTree("null"), out::add);
		assertEquals(1, out.size());
		assertEquals(1, out.get(0).asInt());

		// Second, independent compile on the SAME Environment: `foo` was never re-defined here, and must
		// not have been globally registered as a side effect of the first compile.
		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("foo");
		});
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("foo/0 does not exist"));
	}

	@Test
	public void testLocalDefWithCaptureDoesNotLeakEitherAndFailsCleanlyAfterwards() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		env.compile("1 as $x | def bar: $x; bar");

		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("bar");
		});
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("bar/0 does not exist"));
	}

	@Test
	public void testLocalAstVariableResolution() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(jsonProvider, Versions.JQ_1_7).build();

		JsonQuery<JsonNode> q = env.compile(". as $x | $x");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("123"), out::add);

		assertEquals(1, out.size());
		assertEquals(123, out.get(0).asInt());
	}
}
