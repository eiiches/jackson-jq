package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.comparator.TestJsonNodeComparator;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

		String command = String.format("jq (v%s) '%s' <<< '%s'", version, tc.q, tc.in);
		if (tc.expectations != null) {
			TestCase.Expectation expected = tc.expectations.resolve(version, false, System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
			List<T> values = new ArrayList<>();
			@Var Throwable error = null;
			try {
				JsonQuery<T> query = env.compile(tc.q);
				query.apply(parseTestNode(tc.in, JsonNodeUtils.parseOptions(version)), values::add);
			} catch (Throwable e) {
				error = e;
			}
			assertThat(error != null).as("%s error", command).isEqualTo(expected.error);
			List<T> expectedValues = new ArrayList<>();
			for (JsonNode node : expected.values())
				expectedValues.add(parseExpectedNode(node));
			assertThat(values).as("%s output", command)
					.usingElementComparator(new TestJsonNodeComparator<>(getJsonProvider(), true, tc.floatTolerance))
					.isEqualTo(expectedValues);
			return;
		}

		if (!tc.shouldCompile) {
			assertThatThrownBy(() -> env.compile(tc.q)).isInstanceOf(JsonQueryException.class);
			return;
		}

		// Convert test data from Jackson JsonNode to provider's type
		T input = parseTestNode(tc.in, JsonNodeUtils.parseOptions(version));
		List<T> expectedOut = new ArrayList<>();
		for (JsonNode outNode : tc.out) {
			expectedOut.add(parseExpectedNode(outNode));
		}

		Comparator<T> comparator = new TestJsonNodeComparator<>(getJsonProvider(), true, tc.floatTolerance);

		@Var boolean failed = false;
		try {
			JsonQuery<T> q = env.compile(tc.q);
			List<T> out = new ArrayList<>();
			q.apply(input, out::add);
			assertThat(out).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(expectedOut);
		} catch (Throwable e) {
			failed = true;
			if (!Boolean.TRUE.equals(tc.failing)) {
				if (e instanceof AssertionError)
					throw e;
				e.addSuppressed(new RuntimeException("NOTE: " + command));
				throw e;
			}
		}

		if (Boolean.TRUE.equals(tc.failing))
			assertThat(failed).describedAs("The test case is marked as failing but completed successfully: %s", command).isTrue();
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
			if (tc.expectations != null || tc.appliesTo(jqVersion)) {
				test(tc, jqVersion, moduleSearchPath);
			}
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}
}
