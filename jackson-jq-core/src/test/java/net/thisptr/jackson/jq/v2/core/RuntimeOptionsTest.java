package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuntimeOptionsTest {
	private static final Environment<JsonNode> ENV = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2).build();

	private static List<JsonNode> run(String q, RuntimeOptions options) throws Exception {
		return run(q, options, Jackson2JsonProvider.getInstance().createNull());
	}

	/**
	 * Runs {@code q} against {@code in}.
	 * <p>
	 * Every size-limit case below passes the value under test as input rather than writing it into the query,
	 * because a query that is constant throughout is evaluated once at compile time and folded to its result
	 * -- so it never reaches evaluation, and these budgets meter evaluation. Taking the value as input is what
	 * keeps each case testing the limit rather than the folder. See {@link RuntimeOptions} on what the budgets
	 * do and do not cover, and {@link #constantWorkIsBoundedByTheCompilerNotByTheseLimits}.
	 */
	private static List<JsonNode> run(String q, RuntimeOptions options, JsonNode in) throws Exception {
		List<JsonNode> out = new ArrayList<>();
		ENV.compile(q).withRuntimeOptions(options).apply(in, out::add);
		return out;
	}

	private static JsonNode in(String json) {
		try {
			return new ObjectMapper().readTree(json);
		} catch (Exception e) {
			throw new IllegalArgumentException(json, e);
		}
	}

	private static RuntimeOptions maxArrayLength(int n) {
		return RuntimeOptions.newBuilder().setMaxArrayLength(n).build();
	}

	private static RuntimeOptions maxObjectMemberCount(int n) {
		return RuntimeOptions.newBuilder().setMaxObjectMemberCount(n).build();
	}

	private static RuntimeOptions maxStringLength(int n) {
		return RuntimeOptions.newBuilder().setMaxStringLength(n).build();
	}

	private static RuntimeOptions maxUserDefinedFunctionCalls(long n) {
		return RuntimeOptions.newBuilder().setMaxUserDefinedFunctionCalls(n).build();
	}

	private static RuntimeOptions maxOutputsPerExpression(long n) {
		return RuntimeOptions.newBuilder().setMaxOutputsPerExpression(n).build();
	}

	// --- defaults -----------------------------------------------------------------------------

	@Test
	public void defaultsAreUnlimited() throws Exception {
		RuntimeOptions defaults = RuntimeOptions.newBuilder().build();
		assertThat(defaults.getMaxArrayLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxObjectMemberCount()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxStringLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxBinaryLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(defaults.getMaxUserDefinedFunctionCalls()).isEqualTo(Long.MAX_VALUE);
		assertThat(defaults.getMaxOutputsPerExpression()).isEqualTo(Long.MAX_VALUE);

		assertThat(run("[range(0; 100000)] | length", defaults)).containsExactly(Jackson2JsonProvider.getInstance().createNumber(100000));
		// The no-options overloads must behave identically.
		List<JsonNode> out = new ArrayList<>();
		ENV.compile("[range(0; 100000)] | length").apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(100000));
	}

	@Test
	public void eachSetterLeavesTheOtherLimitsAlone() {
		RuntimeOptions options = RuntimeOptions.newBuilder().setMaxObjectMemberCount(7).setMaxStringLength(5).setMaxBinaryLength(4).setMaxArrayLength(3).setMaxUserDefinedFunctionCalls(9).setMaxOutputsPerExpression(11).build();
		assertThat(options.getMaxArrayLength()).isEqualTo(3);
		assertThat(options.getMaxObjectMemberCount()).isEqualTo(7);
		assertThat(options.getMaxStringLength()).isEqualTo(5);
		assertThat(options.getMaxBinaryLength()).isEqualTo(4);
		assertThat(options.getMaxUserDefinedFunctionCalls()).isEqualTo(9);
		assertThat(options.getMaxOutputsPerExpression()).isEqualTo(11);
	}

	@Test
	public void rejectsNegativeBinaryLength() {
		assertThatThrownBy(() -> RuntimeOptions.newBuilder().setMaxBinaryLength(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxBinaryLength must not be negative");
	}

	// --- maxArrayLength -----------------------------------------------------------------------

	@Test
	public void arrayConstructionIsBounded() throws Exception {
		assertThatCode(() -> run("[range(0; .)]", maxArrayLength(100), in("100"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("[range(0; .)]", maxArrayLength(100), in("101")))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum array size of 100");
	}

	@Test
	public void arrayConcatenationIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; .) as $i ([]; . + [$i])", maxArrayLength(100), in("100"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; .) as $i ([]; . + [$i])", maxArrayLength(100), in("101")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void setpathRejectsAHugeIndexWithoutAllocating() {
		long started = System.nanoTime();
		assertThatThrownBy(() -> run("setpath([1000000000]; 1)", maxArrayLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("Array of 1000000001 elements");
		// The point of pre-checking is that the billion-slot ArrayList is never allocated; if it were,
		// this would take seconds (or OOM) rather than microseconds.
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	@Test
	public void setpathWithinTheLimitStillExtendsArrays() throws Exception {
		// Compared as text: the engine's numeric node type need not match the parser's.
		assertThat(run("setpath([3]; 1)", maxArrayLength(4))).extracting(Object::toString).containsExactly("[null,null,null,1]");
	}

	@Test
	public void sliceAssignmentIsBounded() throws Exception {
		assertThatThrownBy(() -> run(".[0:0] = [1, 2, 3]", maxArrayLength(4), in("[1, 2]")))
				.isInstanceOf(RuntimeLimitExceededException.class);
		assertThatCode(() -> run(".[0:0] = [1, 2]", maxArrayLength(4), in("[1, 2]"))).doesNotThrowAnyException();
	}

	// --- maxObjectMemberCount ------------------------------------------------------------------

	@Test
	public void objectMergeIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; .) as $i ({}; . + {($i|tostring): $i})", maxObjectMemberCount(50), in("50"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; .) as $i ({}; . + {($i|tostring): $i})", maxObjectMemberCount(50), in("51")))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum object size of 50");
	}

	@Test
	public void recursiveObjectMergeIsBounded() throws Exception {
		assertThatThrownBy(() -> run("{a: 1, b: .} * {c: 3}", maxObjectMemberCount(2), in("2")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void objectFieldAssignmentIsBounded() throws Exception {
		assertThatThrownBy(() -> run("reduce range(0; .) as $i ({}; setpath([$i|tostring]; $i))", maxObjectMemberCount(50), in("51")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void fromEntriesIsBounded() throws Exception {
		// from_entries turns an array into an object, so maxArrayLength does not bound the result.
		assertThatThrownBy(() -> run("[range(0; .) | {key: (.|tostring), value: .}] | from_entries", maxObjectMemberCount(50), in("51")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	// --- maxStringLength ----------------------------------------------------------------------

	@Test
	public void stringConcatenationIsBounded() throws Exception {
		assertThatCode(() -> run("reduce range(0; .) as $i (\"\"; . + \"x\")", maxStringLength(100), in("100"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("reduce range(0; .) as $i (\"\"; . + \"x\")", maxStringLength(100), in("101")))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum string length of 100");
	}

	@Test
	public void stringRepetitionIsRejectedWithoutAllocating() {
		long started = System.nanoTime();
		assertThatThrownBy(() -> run("\"x\" * 1000000000", maxStringLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("String of 1000000000 characters");
		// As with setpath, the point of pre-checking is that the billion-char buffer is never
		// allocated; if it were, this would take seconds (or OOM) rather than microseconds.
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	@Test
	public void stringInterpolationIsBounded() throws Exception {
		assertThatCode(() -> run("\"\\(.)\\(.)\"", maxStringLength(6), in("\"aaa\""))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("\"\\(.)\\(.)\"", maxStringLength(5), in("\"aaa\"")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void joinIsBounded() throws Exception {
		assertThatCode(() -> run("join(\"-\")", maxStringLength(7), in("[\"aaa\", \"bbb\"]"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("join(\"-\")", maxStringLength(6), in("[\"aaa\", \"bbb\"]")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void implodeIsBounded() throws Exception {
		assertThatThrownBy(() -> run("[range(0; .) | 65] | implode", maxStringLength(100), in("101")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void formattingFunctionsAreBounded() throws Exception {
		// "aaaa" | @base64 is "YWFhYQ==", eight characters.
		assertThatCode(() -> run("@base64", maxStringLength(8), in("\"aaaa\""))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("@base64", maxStringLength(7), in("\"aaaa\"")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void xsvFormattingIsBounded() throws Exception {
		// ["aaa", "bbb"] | @csv is "\"aaa\",\"bbb\"", eleven characters.
		assertThatCode(() -> run("@csv", maxStringLength(11), in("[\"aaa\", \"bbb\"]"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("@csv", maxStringLength(10), in("[\"aaa\", \"bbb\"]")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void tojsonIsBounded() throws Exception {
		assertThatCode(() -> run("tojson", maxStringLength(7), in("[1, 2, 3]"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("tojson", maxStringLength(6), in("[1, 2, 3]")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void lengthIsCountedInUtf16CodeUnits() throws Exception {
		// An astral character is two chars but one codepoint, so the limit is stricter than jq's
		// length would suggest -- and length itself must keep reporting codepoints.
		assertThat(run("\"\uD83D\uDE00\" | length", RuntimeOptions.newBuilder().build())).extracting(Object::toString).containsExactly("1");
		assertThatCode(() -> run(". + \"\"", maxStringLength(2), in("\"\uD83D\uDE00\""))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run(". + \"\"", maxStringLength(1), in("\"\uD83D\uDE00\"")))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void literalsAndInputsAreNotBounded() throws Exception {
		// The limit bounds strings evaluation produces, not the ones it is handed.
		assertThatCode(() -> run("\"aaaaa\"", maxStringLength(1))).doesNotThrowAnyException();
	}

	// --- what these budgets do not cover -------------------------------------------------------

	/**
	 * These budgets meter evaluation, and a constant expression is not evaluated: the compiler runs it once
	 * and keeps the result, so a query that is constant throughout does its work before any
	 * {@link RuntimeOptions} exists. What bounds that work instead is the compiler's own fixed budget, which
	 * is deliberately small -- past it the fold is abandoned and the expression goes back on the evaluation
	 * path, where these limits apply to it in full.
	 */
	@Test
	public void constantWorkIsBoundedByTheCompilerNotByTheseLimits() throws Exception {
		// Built at compile time, so maxArrayLength never sees it. The same query with the size taken from
		// the input throws -- that is arrayConstructionIsBounded, two tests up.
		assertThatCode(() -> run("[range(0; 101)]", maxArrayLength(100))).doesNotThrowAnyException();

		// Past the compiler's budget the fold is abandoned, and the limit bites after all.
		assertThatThrownBy(() -> run("[range(0; 300)]", maxArrayLength(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum array size of 100");

		// Abandoning a fold is never a compilation failure, and it is what keeps compile() from doing unbounded
		// work on a constant expression: this returns promptly rather than draining a billion values, and it
		// compiles to the same query it always did.
		long started = System.nanoTime();
		assertThatCode(() -> ENV.compile("last(range(0; 1e9))")).doesNotThrowAnyException();
		assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(5);
	}

	// --- maxUserDefinedFunctionCalls ----------------------------------------------------------

	@Test
	public void userDefinedFunctionCallsAreBounded() throws Exception {
		assertThatCode(() -> run("def f: .; [f, f, f]", maxUserDefinedFunctionCalls(3))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("def f: .; [f, f, f]", maxUserDefinedFunctionCalls(2)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 2 user-defined function calls");
	}

	@Test
	public void nestedDefinitionsInTheQueryAreAlsoCounted() throws Exception {
		// `def g` is written in the query text just as much as `def f` is, so both draw on the budget:
		// one call of f plus one of g.
		assertThatCode(() -> run("def f: def g: .; g; f", maxUserDefinedFunctionCalls(2))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("def f: def g: .; g; f", maxUserDefinedFunctionCalls(1)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void runawayRecursionIsBoundedInsteadOfOverflowingTheStack() {
		// `def f: f` is a tail call, so without a budget it loops at constant stack depth and never stops --
		// as jq's own does. This budget, not the Java stack, is what bounds a runaway query-text recursion.
		assertThatThrownBy(() -> run("def f: f; f", maxUserDefinedFunctionCalls(100)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 100 user-defined function calls");
	}

	@Test
	public void valueParametersCountPerBodyExecution() throws Exception {
		// A $-parameter binds each value its argument produces in turn, so one call site runs the body twice.
		assertThatCode(() -> run("def f($a): $a; [f(1, 2)]", maxUserDefinedFunctionCalls(2))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("def f($a): $a; [f(1, 2)]", maxUserDefinedFunctionCalls(1)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void builtinsDoNotDrawOnTheBudget() throws Exception {
		// Even at zero: how a builtin is implemented -- Java, or jq source with its own internal `def`
		// like recurse's `def r` or while's `def _while` -- must not change what the caller's number means.
		assertThatCode(() -> run("[1, 2, 3] | map(. + 1)", maxUserDefinedFunctionCalls(0))).doesNotThrowAnyException();
		assertThatCode(() -> run("[1, 2, 3] | add", maxUserDefinedFunctionCalls(0))).doesNotThrowAnyException();
		assertThatCode(() -> run("1 | [recurse(if . < 5 then . + 1 else empty end)]", maxUserDefinedFunctionCalls(0))).doesNotThrowAnyException();
		assertThatCode(() -> run("1 | [while(. < 5; . + 1)]", maxUserDefinedFunctionCalls(0))).doesNotThrowAnyException();
		assertThatCode(() -> run("1 | until(. > 4; . + 1)", maxUserDefinedFunctionCalls(0))).doesNotThrowAnyException();
	}

	@Test
	public void importedModuleFunctionsDoNotDrawOnTheBudget() throws Exception {
		// A module's `def`s are the module author's, not the caller's -- they compile to the same node as a
		// query-text def but must stay off the budget, exactly like a jq-source builtin.
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.addImportedModule("math", new JqModule<JsonNode>() {
					@Override
					public String getSource() {
						return "def square($x): $x * $x;";
					}

					@Override
					public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
						throw new ModuleNotFoundException(importPath);
					}

					@Override
					public JsonNode relativeData(String importPath, String searchPath) {
						throw new ModuleNotFoundException(importPath);
					}
				})
				.build();

		List<JsonNode> out = new ArrayList<>();
		env.compile("math::square(5)").withRuntimeOptions(maxUserDefinedFunctionCalls(0))
				.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(25));
	}

	@Test
	public void theBudgetStartsOverOnEachInvocation() throws Exception {
		// The tally lives on the per-apply() Memory, so spending it all does not poison the next input.
		JsonQuery<JsonNode> query = ENV.compile("def f: .; [f, f]").withRuntimeOptions(maxUserDefinedFunctionCalls(2));
		for (int i = 0; i < 3; ++i) {
			List<JsonNode> out = new ArrayList<>();
			query.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
			assertThat(out).hasSize(1);
		}
	}

	// --- maxOutputsPerExpression -------------------------------------------------------------

	@Test
	public void expressionOutputsAreBounded() throws Exception {
		assertThatCode(() -> run("1, 2, 3", maxOutputsPerExpression(3))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("1, 2, 3", maxOutputsPerExpression(2)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 2 outputs per expression");
	}

	@Test
	public void aStreamCountsEvenWhenNothingConsumesIt() throws Exception {
		// The work is done whether or not the values survive, so `| empty` must not buy a query its freedom.
		assertThatThrownBy(() -> run("range(0; 1000) | empty", maxOutputsPerExpression(999)))
				.isInstanceOf(RuntimeLimitExceededException.class);
		assertThatThrownBy(() -> run("reduce range(0; 10000000) as $x (0; . + 1)", maxOutputsPerExpression(1000)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 1000 outputs per expression");
	}

	@Test
	public void theBudgetIsPerExpressionNotPerQuery() throws Exception {
		// Twenty values are emitted in all, but no single expression emits more than ten.
		assertThatCode(() -> run("[range(0; 10)] | [range(0; 10)]", maxOutputsPerExpression(10))).doesNotThrowAnyException();
	}

	@Test
	public void anExpressionAccumulatesAcrossEveryReEvaluation() throws Exception {
		// One expression's tally covers the whole invocation, so the right-hand range -- run once per value
		// the left one emits -- spends 600 * 600 while the left spends only 600.
		assertThatCode(() -> run("range(0; 600)", maxOutputsPerExpression(1000))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("range(0; 600) | range(0; 600)", maxOutputsPerExpression(1000)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void builtinInternalsDoNotDrawOnTheOutputBudget() throws Exception {
		// What a builtin emits is charged to the query-text expression that called it, never to the
		// library's own `def`s -- recurse's `def r`, while's `def _while` -- so how a builtin happens to be
		// implemented cannot change what the caller's number means. `limit` stops the recursion by breaking
		// out of it, which costs one value beyond the five it keeps.
		assertThatCode(() -> run("0 | [limit(5; recurse(. + 1))]", maxOutputsPerExpression(6))).doesNotThrowAnyException();
		assertThatCode(() -> run("[1, 2, 3] | map(. + 1)", maxOutputsPerExpression(3))).doesNotThrowAnyException();
	}

	@Test
	public void argumentsHandedToABuiltinAreCharged() throws Exception {
		// The arguments are the caller's own expressions, so a builtin that calls one per iteration spends the
		// caller's budget on it. `while(. < 5; . + 1)` over 1..4 tests the condition five times -- once more
		// than it emits, for the value that ends the loop -- so five is the smallest budget that runs it.
		assertThatCode(() -> run("1 | [while(. < 5; . + 1)]", maxOutputsPerExpression(5))).doesNotThrowAnyException();
		assertThatThrownBy(() -> run("1 | [while(. < 5; . + 1)]", maxOutputsPerExpression(4)))
				.isInstanceOf(RuntimeLimitExceededException.class);
	}

	@Test
	public void aLoopThatEmitsNothingIsStillBounded() throws Exception {
		// `until` emits only its final value, so no amount of output metering downstream can see it looping.
		// What is visible is that it re-evaluates the caller's own `cond` and `update` once per iteration.
		// This is the only thing that bounds it: `until`'s recursion is a tail call, so it runs as a loop at
		// constant stack depth and runs forever without a budget -- as jq's own does. See runtime-limits.md.
		assertThatThrownBy(() -> run("until(false; .)", maxOutputsPerExpression(10)))
				.isInstanceOf(RuntimeLimitExceededException.class)
				.hasMessageContaining("maximum of 10 outputs per expression");
		// Both arguments here constant-fold; metering happens after folding, so they are still charged.
		assertThatThrownBy(() -> run("until(false; 1)", maxOutputsPerExpression(10)))
				.isInstanceOf(RuntimeLimitExceededException.class);
		// A terminating `until` is unaffected: 1..4 tests the condition five times.
		assertThatCode(() -> run("1 | until(. > 4; . + 1)", maxOutputsPerExpression(5))).doesNotThrowAnyException();
	}

	@Test
	public void importedModuleExpressionsDoNotDrawOnTheOutputBudget() throws Exception {
		// A module's expressions are the module author's, not the caller's: only the call written in the
		// query text is tallied, and here it emits one value.
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.addImportedModule("gen", new JqModule<JsonNode>() {
					@Override
					public String getSource() {
						return "def nums: 1, 2, 3, 4, 5;\ndef total: reduce nums as $x (0; . + $x);";
					}

					@Override
					public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
						throw new ModuleNotFoundException(importPath);
					}

					@Override
					public JsonNode relativeData(String importPath, String searchPath) {
						throw new ModuleNotFoundException(importPath);
					}
				})
				.build();

		List<JsonNode> out = new ArrayList<>();
		env.compile("gen::total").withRuntimeOptions(maxOutputsPerExpression(1))
				.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(15));
	}

	@Test
	public void theOutputBudgetStartsOverOnEachInvocation() throws Exception {
		// The tallies live on the per-apply() Memory, so spending them does not poison the next input.
		JsonQuery<JsonNode> query = ENV.compile("1, 2, 3").withRuntimeOptions(maxOutputsPerExpression(3));
		for (int i = 0; i < 3; ++i) {
			List<JsonNode> out = new ArrayList<>();
			query.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
			assertThat(out).hasSize(3);
		}
	}

	// --- the SPI path -------------------------------------------------------------------------

	@Test
	public void aCustomFunctionSeesTheLimitsThroughItsContext() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.defineFunction(FunctionSignature.of("observed_max_array_length", 0), new Function() {
					@Override
					public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
						JsonProvider<N> jsonProvider = bindCtx.getJsonProvider();
						return (context, in, path, output) -> output.emit(jsonProvider.createNumber(context.getRuntimeLimits().getMaxArrayLength()), UntrackedPath.getInstance());
					}
				})
				.build();

		JsonQuery<JsonNode> query = env.compile("observed_max_array_length");
		List<JsonNode> out = new ArrayList<>();
		query.withRuntimeOptions(maxArrayLength(12)).apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
		assertThat(out).containsExactly(Jackson2JsonProvider.getInstance().createNumber(12));
	}

	// --- concurrency --------------------------------------------------------------------------

	@Test
	public void oneCompiledQueryHonoursPerInvocationCallBudgets() throws Exception {
		// The call tally is mutable per-invocation state, unlike the size limits, so prove it neither leaks
		// between threads nor accumulates across invocations: every tight task must fail and every unlimited
		// one must succeed, however they interleave.
		JsonQuery<JsonNode> query = ENV.compile("def f: .; [f, f, f]");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < 64; ++i) {
				boolean tight = i % 2 == 0;
				Callable<Boolean> task = () -> {
					List<JsonNode> out = new ArrayList<>();
					try {
						query.withRuntimeOptions(tight ? maxUserDefinedFunctionCalls(2) : RuntimeOptions.newBuilder().build())
								.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
						return false;
					} catch (RuntimeLimitExceededException e) {
						return true;
					}
				};
				futures.add(executor.submit(task));
			}
			for (int i = 0; i < futures.size(); ++i)
				assertThat(futures.get(i).get()).isEqualTo(i % 2 == 0);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void oneCompiledQueryHonoursPerInvocationLimits() throws Exception {
		JsonQuery<JsonNode> query = ENV.compile("[range(0; .)] | length");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < 64; ++i) {
				boolean tight = i % 2 == 0;
				Callable<Boolean> task = () -> {
					List<JsonNode> out = new ArrayList<>();
					try {
						query.withRuntimeOptions(tight ? maxArrayLength(10) : RuntimeOptions.newBuilder().build())
								.apply(in("50"), out::add);
						return false;
					} catch (RuntimeLimitExceededException e) {
						return true;
					}
				};
				futures.add(executor.submit(task));
			}
			for (int i = 0; i < futures.size(); ++i)
				assertThat(futures.get(i).get()).isEqualTo(i % 2 == 0);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void oneCompiledQueryHonoursPerInvocationOutputBudgets() throws Exception {
		// Like the call tally, the per-expression tallies are mutable per-invocation state: every tight task
		// must fail and every unlimited one must succeed, however they interleave.
		JsonQuery<JsonNode> query = ENV.compile("range(0; 50)");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (int i = 0; i < 64; ++i) {
				boolean tight = i % 2 == 0;
				Callable<Boolean> task = () -> {
					List<JsonNode> out = new ArrayList<>();
					try {
						query.withRuntimeOptions(tight ? maxOutputsPerExpression(10) : RuntimeOptions.newBuilder().build())
								.apply(Jackson2JsonProvider.getInstance().createNull(), out::add);
						return false;
					} catch (RuntimeLimitExceededException e) {
						return true;
					}
				};
				futures.add(executor.submit(task));
			}
			for (int i = 0; i < futures.size(); ++i)
				assertThat(futures.get(i).get()).isEqualTo(i % 2 == 0);
		} finally {
			executor.shutdownNow();
		}
	}
}
