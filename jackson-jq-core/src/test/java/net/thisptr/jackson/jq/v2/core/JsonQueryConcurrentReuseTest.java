package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test guarding against "frame corruption" bugs: a single compiled {@link JsonQuery} must be
 * safely reusable from many threads concurrently, because {@code RootExpression.apply} allocates a brand
 * new stack frame on every top-level call and never mutates shared state on the compiled AST or
 * {@link Environment} after {@code compile()} returns (see {@code docs/stack-frame-closure-design.md}).
 *
 * <p>The query nests three levels of {@code def} with {@code $x} shadowed at each level, plus an unbound
 * (non-{@code $}) positional parameter referenced multiple times -- each reference lazily re-invokes the
 * captured argument as a generator/closure. This exercises the per-call frame-slot allocation and
 * closure-capture machinery that a real cross-thread frame-sharing bug would corrupt.
 *
 * <p>The query is parameterized by a declared {@code $seed} variable. Two immutable query views supply
 * distinguishable values via {@link RuntimeBindings}, and each view's prepared global bindings are reused
 * concurrently. A single fixed value would be a blind spot: if one view's state leaked into another, both
 * would still expect the same result. With two distinguishable patterns, a leak surfaces as one pattern's
 * call producing the other pattern's result.
 */
public class JsonQueryConcurrentReuseTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	private static final String QUERY =
			"def id(x): x; " +
					"$seed as $x | " +
					"def f(x): 1 as $x | id([$x, x, x]); " +
					"def g(x): 100 as $x | f($x, $x+x); " +
					"range(0; 1) as $_ | g($x)";

	private static final int SEED_A = 2000;
	private static final int SEED_B = -13000;

	private static final int THREAD_COUNT = 8;
	private static final int TOTAL_INVOCATIONS = 10000;

	@Test
	public void producesResultsMatchingTheirOwnBindingsWhenReusedConcurrently() throws Exception {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("seed")
				.build();
		JsonQuery<JsonNode> query = env.compile(QUERY);
		JsonQuery<JsonNode> queryA = withSeed(query, SEED_A);
		JsonQuery<JsonNode> queryB = withSeed(query, SEED_B);

		List<JsonNode> goldenA = run(queryA);
		List<JsonNode> goldenB = run(queryB);
		assertThat(goldenA).as("sanity check: pattern A must produce output").isNotEmpty();
		assertThat(goldenB).as("sanity check: pattern B must produce output").isNotEmpty();
		assertThat(goldenA).as("the two patterns must differ, or cross-invocation corruption would be undetectable")
				.isNotEqualTo(goldenB);

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		try {
			List<Future<List<JsonNode>>> futures = new ArrayList<>();
			List<Integer> seeds = new ArrayList<>();
			for (int i = 0; i < TOTAL_INVOCATIONS; i++) {
				int seed = (i % 2 == 0) ? SEED_A : SEED_B;
				seeds.add(seed);
				JsonQuery<JsonNode> selectedQuery = seed == SEED_A ? queryA : queryB;
				futures.add(executor.submit(() -> run(selectedQuery)));
			}

			List<String> mismatches = new ArrayList<>();
			for (int i = 0; i < futures.size(); i++) {
				List<JsonNode> expected = (seeds.get(i) == SEED_A) ? goldenA : goldenB;
				List<JsonNode> result = futures.get(i).get();
				if (!expected.equals(result)) {
					mismatches.add("iteration " + i + " (seed=" + seeds.get(i) + "): expected " + expected + " but was " + result);
				}
			}
			assertThat(mismatches)
					.as("%d/%d concurrent invocations diverged from the expected per-pattern golden result",
							mismatches.size(), TOTAL_INVOCATIONS)
					.isEmpty();
		} finally {
			executor.shutdownNow();
		}
	}

	private static JsonQuery<JsonNode> withSeed(JsonQuery<JsonNode> query, int seed) throws JsonQueryException {
		RuntimeBindings<JsonNode> bindings = RuntimeBindings.<JsonNode>newBuilder()
				.setVariable("seed", JSON_PROVIDER.createNumber(seed))
				.build();
		return query.withRuntimeBindings(bindings);
	}

	private static List<JsonNode> run(JsonQuery<JsonNode> query) throws JsonQueryException {
		List<JsonNode> result = new ArrayList<>();
		query.apply(JSON_PROVIDER.createNull(), result::add);
		return result;
	}
}
