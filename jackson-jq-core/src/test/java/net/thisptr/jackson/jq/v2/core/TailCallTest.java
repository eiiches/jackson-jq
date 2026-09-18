package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Depth, and what it costs.
 * <p>
 * The jq-level semantics of a tail call are pinned by {@code tests/test-cases/constructs/tail-call.yaml},
 * which is checked against the real jq binaries. What cannot go there is anything about the Java stack: how
 * deep a recursion may go is this engine's own property, not jq's, and the version ranges in those files
 * describe jq's behaviour. So the cases here are the ones that would exhaust the stack without the
 * optimization, and the ones that say what the optimization costs a budget.
 */
class TailCallTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final Environment<JsonNode> ENV = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).build();

	/**
	 * Comfortably past the few hundred iterations the Java stack allows, and small enough to stay quick.
	 */
	private static final int DEEP = 100_000;

	private static final CompileOptions WITHOUT_TAIL_CALLS = CompileOptions.newBuilder()
			.setTailCallOptions(TailCallOptions.newBuilder().setEnabled(false).build())
			.build();

	private static List<JsonNode> run(String q) throws JsonQueryException {
		return run(q, CompileOptions.newBuilder().build());
	}

	private static List<JsonNode> run(String q, CompileOptions options) throws JsonQueryException {
		List<JsonNode> out = new ArrayList<>();
		ENV.compile(q, options).apply(JSON_PROVIDER.createNull(), out::add);
		return out;
	}

	private static List<JsonNode> run(String q, RuntimeOptions options) throws JsonQueryException {
		List<JsonNode> out = new ArrayList<>();
		ENV.compile(q).withRuntimeOptions(options).apply(JSON_PROVIDER.createNull(), out::add);
		return out;
	}

	private static String number(List<JsonNode> out) {
		assertThat(out).hasSize(1);
		return out.get(0).toString();
	}

	@Test
	void aRecursiveDefRunsAtConstantStackDepth() throws JsonQueryException {
		assertThat(number(run("0 | def f: if . < " + DEEP + " then . + 1 | f else . end; f"))).isEqualTo(String.valueOf(DEEP));
	}

	@Test
	void aValueParameterIsBoundAtTheCallSiteAndTheCallIsStillAJump() throws JsonQueryException {
		assertThat(number(run("0 | def f($n): if $n == 0 then . else . + 1 | f($n - 1) end; f(" + DEEP + ")"))).isEqualTo(String.valueOf(DEEP));
	}

	@Test
	void aFilterParameterPassedStraightOnDoesNotAccumulate() throws JsonQueryException {
		// Not merely a depth check: the ordinary call path wraps a filter argument in a fresh Function per
		// call, so passing a parameter along a recursion this way builds one wrapper per iteration and
		// invoking it at the end costs exactly the frames the loop saved. A tail call hands the Function on
		// as it is, the way jq copies a closure reference.
		assertThat(number(run("0 | def w(f): if . < " + DEEP + " then . + 1 | w(f) else f end; w(. * 2)"))).isEqualTo(String.valueOf(2 * DEEP));
	}

	@Test
	void mutualTailRecursionRunsInOneLoop() throws JsonQueryException {
		// jq can only express this by nesting: a def sees only the defs that precede it.
		String q = DEEP + " | def outer: def inner: if . > 0 then . - 1 | outer else \"inner\" end;"
				+ " if . > 0 then . - 1 | inner else \"outer\" end; outer";
		assertThat(number(run(q))).isEqualTo("\"outer\"");
	}

	@Test
	void frameReuseDoesNotLeakStateBetweenIterations() throws JsonQueryException {
		// A call back into the def it is already in reuses the frame, so the previous iteration's locals are
		// still sitting in their slots -- a nested def's, and the `as` binding in the condition here. That is
		// safe only because a name is in scope only inside its binder, so nothing can read a slot before the
		// construct that writes it has run in that same iteration. This is what pins it.
		String query = "0 | def f: def g: . * 100; if (. as $x | $x) < %d then . + 1 | f else g end; f";

		// Shallow enough that both settings can run it, so they can be compared at all.
		assertThat(run(String.format(query, 8))).isEqualTo(run(String.format(query, 8), WITHOUT_TAIL_CALLS));
		assertThat(number(run(String.format(query, DEEP)))).isEqualTo(String.valueOf(100L * DEEP));
	}

	@Test
	void aMultiValueDollarArgumentIsolatesItsBodyExecutions() throws JsonQueryException {
		// f(1, 2; ...) runs the body twice, each execution getting its own frame. The tail call inside writes
		// the parameter slots of the frame it is in, so the second execution must not see what the first left
		// -- including the filter parameter it passes along untouched.
		String query = "0 | def f($n; g): if $n == 0 then g else . + 1 | f($n - 1; g) end; [f(1, 2; . * 10)]";

		assertThat(run(query).toString()).isEqualTo("[[10,20]]");
		assertThat(run(query)).isEqualTo(run(query, WITHOUT_TAIL_CALLS));
	}

	@Test
	void crossTargetHopsStayFlatWhenTheyCarryArguments() throws JsonQueryException {
		// Alternating targets every iteration, so the loop replaces the frame and installs a fresh argument on
		// every one of them. One loop drains the whole chain; a loop per def would cost a Java frame per hop.
		String query = "def a($n): def b($m): if $m > 0 then a($m - 1) else \"b\" end;"
				+ " if $n > 0 then b($n - 1) else \"a\" end; a(" + DEEP + ")";

		assertThat(number(run(query))).isEqualTo("\"a\"");
	}

	@Test
	void theJqDefinedLoopingBuiltinsIterateAsFarAsJqs() throws JsonQueryException {
		assertThat(number(run("1 | until(. > " + DEEP + "; . + 1)"))).isEqualTo(String.valueOf(DEEP + 1));
		assertThat(number(run("1 | last(while(. < " + DEEP + "; . + 1))"))).isEqualTo(String.valueOf(DEEP - 1));
		assertThat(number(run("0 | [limit(" + DEEP + "; recurse(. + 1))] | length"))).isEqualTo(String.valueOf(DEEP));
	}

	@Test
	void aTailCallInsideTryStillRaisesInsideIt() throws JsonQueryException {
		assertThat(number(run("0 | def f: if . < " + DEEP + " then . + 1 | f else error(\"boom\") end; [try f catch .] | length")))
				.isEqualTo("1");
	}

	@Test
	void aBreakStillUnwindsToItsOwnLabel() throws JsonQueryException {
		assertThat(number(run("0 | [label $out | (def f: if . < " + DEEP + " then . + 1 | f else . end; f), break $out] | length")))
				.isEqualTo("1");
	}

	@Test
	void turningTheOptimizationOffBringsBackTheStackOverflow() {
		String q = "0 | def f: if . < " + DEEP + " then . + 1 | f else . end; f";

		assertThatThrownBy(() -> run(q, WITHOUT_TAIL_CALLS))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Stack overflow during evaluation");
	}

	@Test
	void turningTheOptimizationOffChangesNothingElse() throws JsonQueryException {
		// Both settings have to agree on every shape, whether or not the optimization applies to it: the
		// switch exists to isolate the optimization, not to choose between behaviours.
		List<String> queries = Arrays.asList(
				"0 | def f: if . < 8 then . + 1 | f else . end; [f]",
				"2 | def f: if . > 0 then (. - 1, . - 1) | f else . end; [f]",
				"2 | def f: if . > 0 then ((. - 1 | f), \"after\") else \"base\" end; [f]",
				"0 | def f($n): if $n == 0 then . else . + 1 | f($n - 1) end; [f(1, 2)]",
				"0 | def g(h): if . < 3 then . + 1 | g(h) else h end; [g(. * 10)]",
				"[limit(6; while(true; . + 1))]",
				"[recurse(if . < 4 then . + 1 else empty end)]",
				"{\"a\":{\"b\":{\"c\":1}}} | def f: if type == \"object\" then .[] | f else . end; [path(f)]");

		for (String q : queries) {
			assertThat(run(q)).as(q).isEqualTo(run(q, WITHOUT_TAIL_CALLS));
		}
	}

	@Test
	void everyIterationStillCostsOneUserDefinedFunctionCall() throws JsonQueryException {
		RuntimeOptions budget = RuntimeOptions.newBuilder().setMaxUserDefinedFunctionCalls(10).build();

		assertThatThrownBy(() -> run("0 | def f: if . < 100 then . + 1 | f else . end; f", budget))
				.isInstanceOf(RuntimeLimitExceededException.class);

		// Nine iterations plus the call that starts it stays inside a budget of ten, so the limit is being
		// spent per iteration rather than once for the whole loop.
		assertThat(number(run("0 | def f: if . < 9 then . + 1 | f else . end; f", budget))).isEqualTo("9");
	}

	@Test
	void aLibraryLoopIsStillBoundedByTheOutputBudget() {
		// Nothing counts a library def's calls, by design, so what stops until(false; .) now that the stack
		// no longer does is the budget on what its arguments emit -- once per iteration. See runtime-limits.md.
		RuntimeOptions budget = RuntimeOptions.newBuilder().setMaxOutputsPerExpression(1000).build();

		assertThatThrownBy(() -> run("until(false; .)", budget)).isInstanceOf(RuntimeLimitExceededException.class);
		assertThatThrownBy(() -> run("until(false; 1)", budget)).isInstanceOf(RuntimeLimitExceededException.class);
	}
}
