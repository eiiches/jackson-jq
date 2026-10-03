package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.ParseOptions;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.comparator.TestJsonNodeComparator;
import net.thisptr.jackson.jq.v2.test.evaluator.EvaluationLimits;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Abstract base class for JsonQuery tests. Subclasses must implement methods to provide
 * the JsonProvider-specific environment and comparator.
 *
 * <p>This class is designed to be extended by JSON provider implementations (e.g., jackson-jq-jackson2)
 * to run the standard test suite against their implementation.
 *
 * @param <T> The JSON node type used by the JsonProvider implementation
 */
public abstract class AbstractJsonQueryTest<T> {
	/**
	 * The JsonProvider under test.
	 *
	 * @return The provider implementation to run the test suite against
	 */
	protected abstract JsonProvider<T> getJsonProvider();

	/**
	 * Parse a Jackson JsonNode (from test data) to the provider's native type.
	 *
	 * @param node The Jackson JsonNode from test data
	 * @param parseOptions how the jq version under test holds the numbers it reads
	 * @return The equivalent node in the provider's type
	 */
	protected abstract T parseTestNode(JsonNode node, ParseOptions parseOptions);

	/**
	 * Converts an expectation, which is already written as the result and so needs no rounding.
	 */
	private T parseExpectedNode(JsonNode node) {
		return parseTestNode(node, ParseOptions.newBuilder().build());
	}

	private void test(TestCase tc, Version version, @Nullable Path moduleSearchPath) {
		EnvironmentBuilder<T> envBuilder = EnvironmentBuilder.withDefaultLoaders(getJsonProvider(), version);
		if (moduleSearchPath != null) {
			envBuilder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), moduleSearchPath))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		Environment<T> env = envBuilder
				// Regex is an extension module; the suite includes it because jq's test cases call
				// test, match, sub and the rest by their bare names.
				.includeModule(new JoniRegexModule())
				.defineVariable("ENV", () -> envBuilder.getJsonProvider().createObject(Collections.singletonMap("PAGER", envBuilder.getJsonProvider().createString("less"))))
				.build();

		String command = String.format("jq (v%s) '%s' <<< '%s'", version, tc.q, tc.input);
		TestCase.AbstractExpectation expected = tc.expectations.resolve(version, false, System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
		boolean explicitJjq = tc.expectations.hasJjq(version);
		if (expected.timedOut() || expected.unstable())
			return;
		List<T> values = new ArrayList<>();
		@Var Throwable error = null;
		@Var boolean compiled = false;
		try {
			JsonQuery<T> query = env.compile(tc.q);
			compiled = true;
			query.withRuntimeOptions(EvaluationLimits.OPTIONS)
					.apply(parseTestNode(tc.input, JsonNodeUtils.parseOptions(version)), values::add);
		} catch (Throwable e) {
			error = e;
		}
		if (!explicitJjq && (expected.compileError != null || expected.runtimeError != null)) {
			assertThat(error).as("%s: expected an error", command).isNotNull()
					.isNotInstanceOf(RuntimeLimitExceededException.class);
			return;
		}
		if (expected.limitExceeded()) {
			assertThat(error).as("%s: expected a runtime limit to be exceeded", command)
					.isInstanceOf(RuntimeLimitExceededException.class);
		} else {
			assertThat(error instanceof RuntimeLimitExceededException).as("%s: unexpected runtime limit", command).isFalse();
		}
		if (expected.compileError != null) {
			assertThat(compiled).as("%s: expected compilation to fail", command).isFalse();
			assertThat(error).as("%s: expected a compilation error", command).isNotNull();
			return;
		}
		assertThat(compiled).as("%s: expected compilation to succeed, but got %s", command, error).isTrue();
		if (expected.runtimeError != null)
			assertThat(error).as("%s: expected an evaluation error, but query completed with output %s", command, values).isNotNull();
		else if (!expected.limitExceeded())
			assertThat(error).as("%s: expected successful execution", command).isNull();
		List<T> expectedValues = new ArrayList<>();
		for (JsonNode node : expected.values())
			expectedValues.add(parseExpectedNode(node));
		assertThat(values).as("%s output", command)
				.usingElementComparator(new TestJsonNodeComparator<>(getJsonProvider(), true, tc.floatTolerance))
				.isEqualTo(expectedValues);
	}

	protected final void run(String[] args) throws IOException {
		if (args.length != 1)
			throw new IllegalArgumentException(String.format("Usage: %s <test-case-resource>", getClass().getSimpleName()));

		assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.flatMap(tcText -> Versions.versions().stream()
								.map(version -> (Executable) () -> testVersion(tcText, version))));
	}

	private void testVersion(String tcText, Version jqVersion) throws Throwable {
		TestCase tc = TestCaseLoader.parseTestCase(tcText);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			test(tc, jqVersion, moduleSearchPath);
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}
}
