package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.JqExecutables;
import net.thisptr.jackson.jq.v2.test.evaluator.JqRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Verifies that the {@code out} expectations in the golden test data under
 * {@code src/test/resources/tests} actually match what the real {@code jq} CLI produces.
 *
 * <p>This is deliberately independent of any {@link net.thisptr.jackson.jq.v2.spi.JsonProvider}
 * (unlike {@link AbstractJsonQueryTest}, which checks this library's own implementation against
 * the same golden data), so it only needs to run once rather than once per JsonProvider module.
 *
 * <p>Only checked against the versions the library actually implements ({@link Versions#versions()}),
 * not every {@link JqExecutables#ALL} entry: golden-data cases with no explicit {@code v:} range
 * apply to "any version", and real jq's behavior has genuinely changed across 1.7/1.8.x in ways the
 * (mostly 1.5/1.6-sourced) golden data was never scoped for. Widening this needs a dedicated pass to
 * research and annotate each behavior change with the correct {@code v:} range.
 */
public class VerifyTestCasesTest {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	protected static Stream<String> defaultTestCases() throws IOException {
		return TestCaseLoader.loadAllTestCasesAsJsonStrings();
	}

	private void verify(TestCase tc, JqExecutables.JqExecutable e, @Nullable Path moduleSearchPath) throws Throwable {
		String command = String.format("%s '%s' <<< '%s'", e.executable, tc.q, tc.in);

		Evaluator.Result result = new JqRunner(e.executable, moduleSearchPath).evaluate(tc.q, tc.in, Duration.ofMillis(2000));
		assertThat(result.error).as("%s", command).isNull();

		Comparator<JsonNode> comparator = new JsonNodeComparatorForTests(!tc.ignoreFieldOrder, tc.numericalErrors);
		assertThat(tc.out).as("%s", command)
				.usingElementComparator(comparator)
				.isEqualTo(result.values);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void test(String tcText) throws Throwable {
		TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			List<Executable> testExecutables = new ArrayList<>();
			for (JqExecutables.JqExecutable e : JqExecutables.ALL) {
				if (tc.version == null || tc.version.contains(e.jqVersion)) {
					if (!tc.shouldCompile || tc.ignoreTrueJqBehavior) {
						testExecutables.add(() -> {
							assertThat(catchThrowable(() -> verify(tc, e, moduleSearchPath)))
									.describedAs("Test case marked as should_compile = false or ignore_true_jq_behavior = true should fail against actual jq.")
									.isInstanceOf(Throwable.class);
						});
					} else {
						testExecutables.add(() -> verify(tc, e, moduleSearchPath));
					}
				} else {
					testExecutables.add(() -> {
						assertThat(catchThrowable(() -> verify(tc, e, moduleSearchPath)))
								.describedAs("The version range excludes %s, but the test case succeeds anyway: %s", e.jqVersion, tcText)
								.isInstanceOf(Throwable.class);
					});
				}
			}
			Assertions.assertAll(testExecutables);
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}
}
