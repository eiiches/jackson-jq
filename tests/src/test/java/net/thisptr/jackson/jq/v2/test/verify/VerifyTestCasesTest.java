package net.thisptr.jackson.jq.v2.test.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.comparator.TestJsonNodeComparator;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.JqExecutables;
import net.thisptr.jackson.jq.v2.test.evaluator.JqRunner;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Verifies that the expectations in the golden test data under
 * {@code tests/test-cases} actually match what the real {@code jq} CLI produces.
 *
 * <p>This is deliberately independent of any {@link net.thisptr.jackson.jq.v2.json.JsonProvider}
 * (unlike {@link AbstractJsonQueryTest}, which checks this library's own implementation against
 * the same golden data), so it only needs to run once rather than once per JsonProvider module.
 *
 * <p>Every case must cover every supported jq version and known OS/architecture pair. A
 * default row covers every pair unless a matching platform override replaces it.
 */
public class VerifyTestCasesTest {
	// The downloader gives jq 20 seconds. A recorded timeout should still be slow after 4 seconds,
	// while a recorded result gets 100 seconds so a slower verification machine can finish it.
	private static final Duration EXPECTED_TIMEOUT_CHECK = Duration.ofSeconds(4);
	private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(100);

	static void validateVersionRanges(TestCase tc, Version latestVersion) {
		for (TestCase.AbstractExpectation row : tc.expectations.defaultRows)
			validateVersionRange("expectations.default", row.version, latestVersion);
		for (TestCase.AbstractExpectation row : tc.expectations.overrides)
			validateVersionRange("expectations.overrides", row.version, latestVersion);
		for (TestCase.AbstractExpectation row : tc.expectations.jjq)
			validateVersionRange("expectations.jjq", row.version, latestVersion);
		for (TestCase.TypeAssertion row : tc.types)
			validateVersionRange("types", row.version, latestVersion);
		for (TestCase.PropertyAssertion row : tc.properties)
			validateVersionRange("properties", row.version, latestVersion);
	}

	private static void validateVersionRange(String group, VersionRange range, Version latestVersion) {
		if (range.minVersion() == null || !range.minInclusive())
			throw new IllegalArgumentException(group + " range must have an inclusive start: " + range);
		if (range.maxVersion() != null && range.contains(latestVersion))
			throw new IllegalArgumentException(group + " range covering jq " + latestVersion + " must have an open end: " + range);
		if (range.maxInclusive())
			throw new IllegalArgumentException(group + " range must have an exclusive end: " + range);
	}

	private void verify(TestCase tc, JqExecutables.JqExecutable e, @Nullable Path moduleSearchPath) throws Throwable {
		String command = String.format("%s '%s' <<< '%s'", e.executable(), tc.q, tc.input);
		TestCase.AbstractExpectation expected = tc.expectations.resolve(e.jqVersion(), true, System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
		if (expected.unstable())
			return;
		boolean timedOut = expected.timedOut();
		Duration timeout = timedOut ? EXPECTED_TIMEOUT_CHECK : COMPLETION_TIMEOUT;

		Evaluator.Result result;
		try {
			result = new JqRunner(e.executable(), moduleSearchPath).evaluate(tc.q, tc.input, timeout);
		} catch (TimeoutException timeoutException) {
			if (timedOut)
				return;
			throw new AssertionError(String.format("jq timed out after %s: %s", timeout, command), timeoutException);
		}
		if (timedOut)
			throw new AssertionError(String.format("jq completed instead of timing out after %s: %s", timeout, command));
		Evaluator.ErrorPhase expectedPhase = expected.compileError != null ? Evaluator.ErrorPhase.COMPILE
				: expected.runtimeError != null ? Evaluator.ErrorPhase.RUNTIME : null;
		assertThat(result.errorPhase()).as("%s error phase (stderr: %s)", command, result.stderr()).isEqualTo(expectedPhase);

		Comparator<JsonNode> comparator = new TestJsonNodeComparator<>(Jackson2JsonProvider.getInstance(), true, tc.floatTolerance);
		assertThat(expected.output == null ? List.<JsonNode>of() : expected.values()).as("%s", command)
				.usingElementComparator(comparator)
				.isEqualTo(result.values());
	}

	public void test(String tcText) throws Throwable {
		TestCase tc = TestCaseLoader.parseTestCase(tcText);
		List<Version> versions = Versions.versions();
		try {
			validateVersionRanges(tc, versions.get(versions.size() - 1));
			tc.expectations.validateCoverage(versions);
			tc.expectations.validateNoRedundantOverrides(versions, tc.floatTolerance);
		} catch (IllegalArgumentException failure) {
			throw new IllegalArgumentException(tc.describe() + ": " + failure.getMessage(), failure);
		}
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			List<Executable> testExecutables = new ArrayList<>();
			for (JqExecutables.JqExecutable e : JqExecutables.ALL)
				testExecutables.add(() -> verify(tc, e, moduleSearchPath));
			assertAll(testExecutables);
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1)
			throw new IllegalArgumentException("Usage: VerifyTestCasesTest <test-case-resource>");

		VerifyTestCasesTest verifier = new VerifyTestCasesTest();
		assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.map(tcText -> (Executable) () -> verifier.test(tcText)));
	}
}
