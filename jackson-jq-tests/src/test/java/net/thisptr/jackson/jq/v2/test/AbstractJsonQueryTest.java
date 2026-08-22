package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * Create an EnvironmentBuilder for the given version, ready for any additional configuration
	 * (e.g. the {@code ENV} variable added in {@link #test}) before {@code build()}.
	 *
	 * @param version The jq version to use
	 * @return A configured builder ready for query compilation
	 */
	protected abstract EnvironmentBuilder<T> createEnvironment(Version version);

	/**
	 * Parse a Jackson JsonNode (from test data) to the provider's native type.
	 *
	 * @param node The Jackson JsonNode from test data
	 * @return The equivalent node in the provider's type
	 */
	protected abstract T parseTestNode(JsonNode node);

	/**
	 * Create a comparator for comparing output nodes.
	 *
	 * @param strictFieldOrder Whether to enforce strict field ordering in objects
	 * @param numericalErrors  Allowed numerical error tolerance
	 * @return A comparator for the provider's node type
	 */
	protected abstract Comparator<T> createComparator(boolean strictFieldOrder, double numericalErrors);

	private void test(TestCase tc, Version version, @Nullable Path moduleSearchPath) throws Throwable {
		EnvironmentBuilder<T> envBuilder = createEnvironment(version);
		if (moduleSearchPath != null) {
			envBuilder.setModuleLoader(new ChainedModuleLoader<>(
					new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), version, moduleSearchPath),
					ClassPathModuleLoader.getInstance()));
		}
		Environment<T> env = envBuilder
				.defineVariable("ENV", () -> {
					T envObj = envBuilder.getJsonProvider().createObject();
					envBuilder.getJsonProvider().set(envObj, "PAGER", envBuilder.getJsonProvider().createString("less"));
					return envObj;
				})
				.build();

		String command = String.format("jq (v%s) '%s' <<< '%s'", version, tc.q, tc.in);

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> env.compile(tc.q));
			return;
		}

		// Convert test data from Jackson JsonNode to provider's type
		T input = parseTestNode(tc.in);
		List<T> expectedOut = new ArrayList<>();
		for (JsonNode outNode : tc.out) {
			expectedOut.add(parseTestNode(outNode));
		}

		Comparator<T> comparator = createComparator(!tc.ignoreFieldOrder, tc.numericalErrors);

		@Var boolean failed = false;
		try {
			JsonQuery<T> q = env.compile(tc.q);
			List<T> out = new ArrayList<>();
			q.apply(input, (val, path) -> out.add(val));
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

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_5(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_5);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_6(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_6);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_7(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_7);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_7_1(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_7_1);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_8_0(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_8_0);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_8_1(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_8_1);
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void testJq1_8_2(String tcText) throws Throwable {
		testVersion(tcText, Versions.JQ_1_8_2);
	}

	protected static Stream<String> defaultTestCases() throws IOException {
		return TestCaseLoader.loadAllTestCasesAsJsonStrings();
	}

	private void testVersion(String tcText, Version jqVersion) throws Throwable {
		TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			if (tc.version == null || tc.version.contains(jqVersion)) {
				test(tc, jqVersion, moduleSearchPath);
			}
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}
}
