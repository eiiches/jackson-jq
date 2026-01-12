package net.thisptr.jackson.jq.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.VersionRange;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.test.VersionRangeDeserializer;
import net.thisptr.jackson.jq.test.evaluator.CachedEvaluator;
import net.thisptr.jackson.jq.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.test.evaluator.Evaluator.Result;
import net.thisptr.jackson.jq.test.evaluator.TrueJqEvaluator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for JsonQuery tests. Subclasses must implement methods to provide
 * the JsonProvider-specific scope and comparator.
 *
 * <p>This class is designed to be extended by JSON provider implementations (e.g., jackson-jq-jackson2)
 * to run the standard test suite against their implementation.
 *
 * @param <T> The JSON node type used by the JsonProvider implementation
 */
public abstract class AbstractJsonQueryTest<T> {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TestCase {
		@JsonProperty("q")
		public String q;

		@JsonProperty("in")
		public JsonNode in;

		@JsonProperty("out")
		public List<JsonNode> out;

		@JsonProperty("file")
		public String file;

		@JsonProperty("failing")
		public Boolean failing;

		@JsonProperty("should_compile")
		public boolean shouldCompile = true;

		@JsonProperty("ignore_true_jq_behavior")
		public boolean ignoreTrueJqBehavior = false;

		@JsonProperty("numerical_errors")
		public double numericalErrors = 0;

		@JsonProperty("ignore_field_order")
		public boolean ignoreFieldOrder = false;

		@JsonInclude(Include.NON_NULL)
		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public VersionRange version;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", q, in, out, version != null ? version : "any");
		}
	}

	/**
	 * Create a root scope for the given version. The scope should have all built-in functions loaded.
	 *
	 * @param version The jq version to use
	 * @return A configured scope ready for query execution
	 */
	protected abstract Scope<T> createRootScope(Version version);

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
	 * @param numericalErrors Allowed numerical error tolerance
	 * @return A comparator for the provider's node type
	 */
	protected abstract Comparator<T> createComparator(boolean strictFieldOrder, double numericalErrors);

	private static List<TestCase> loadTestCases(final Path path, final boolean failing) throws IOException {
		try (InputStream in = Files.newInputStream(path)) {
			return loadTestCases(path.toString(), in, failing);
		}
	}

	private static List<TestCase> loadTestCases(final String resourceName, final InputStream in, final boolean failing) throws IOException {
		final TestCase[] result;
		if (resourceName.endsWith(".yaml")) {
			result = YAML_MAPPER.readValue(in, TestCase[].class);
		} else if (resourceName.endsWith(".json")) {
			result = JSON_MAPPER.readValue(in, TestCase[].class);
		} else {
			throw new IllegalArgumentException("unsupported file format");
		}
		for (final TestCase tc : result) {
			if (tc.failing == null)
				tc.failing = failing;
			tc.file = resourceName;
		}
		return Arrays.asList(result);
	}

	private static final String TEST_DIR_PROPERTY = "jq.testcase.dir";

	protected static Stream<String> defaultTestCases() throws IOException {
		final String testDirPath = System.getProperty(TEST_DIR_PROPERTY);
		if (testDirPath == null) {
			throw new IllegalStateException("System property " + TEST_DIR_PROPERTY + " must be set");
		}

		final Path testDir = Paths.get(testDirPath);
		final List<TestCase> testCases = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(testDir)) {
			paths.filter(Files::isRegularFile)
				.filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".yaml"))
				.forEach(path -> {
					try {
						testCases.addAll(loadTestCases(path, false));
					} catch (final IOException e) {
						throw new RuntimeException("Failed to load " + path, e);
					}
				});
		}

		return testCases.stream().map(tc -> {
			try {
				return JSON_MAPPER.writeValueAsString(tc);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static Map<Version, Boolean> hasJqCache = new ConcurrentHashMap<>();
	private static Evaluator cachedJqEvaluator;

	@BeforeAll
	static void beforeAll() {
		cachedJqEvaluator = new CachedEvaluator(new TrueJqEvaluator(), "/tmp/jackson-jq-test.cache");
	}

	@AfterAll
	static void afterAll() throws Exception {
		if (cachedJqEvaluator instanceof AutoCloseable)
			((AutoCloseable) cachedJqEvaluator).close();
	}

	@SuppressWarnings("unchecked")
	private void test(final TestCase tc, final Version version) throws Throwable {
		final Scope<T> scope = createRootScope(version);
		final String command = String.format("%s '%s' <<< '%s'", TrueJqEvaluator.executable(version), tc.q, tc.in);

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> JsonQuery.compile(tc.q, version));
			return;
		}

		// Convert test data from Jackson JsonNode to provider's type
		final T input = parseTestNode(tc.in);
		final List<T> expectedOut = new ArrayList<>();
		for (JsonNode outNode : tc.out) {
			expectedOut.add(parseTestNode(outNode));
		}

		final Comparator<T> comparator = createComparator(!tc.ignoreFieldOrder, tc.numericalErrors);

		if (!tc.ignoreTrueJqBehavior && hasJqCache.computeIfAbsent(version, v -> TrueJqEvaluator.hasJq(v))) {
			final Result result = cachedJqEvaluator.evaluate(tc.q, tc.in, version, 2000L);
			try {
				assertThat(result.error).as("%s", command).isNull();
				// Compare with true jq output (which uses Jackson JsonNode)
				List<T> trueJqOut = new ArrayList<>();
				for (JsonNode outNode : result.values) {
					trueJqOut.add(parseTestNode(outNode));
				}
				assertThat(expectedOut).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(trueJqOut);
			} catch (AssertionError e) {
				Assumptions.abort(String.format("Assumption failed: %s %s", command, e));
			}
		}

		boolean failed = false;
		try {
			final JsonQuery<T> q = JsonQuery.compile(tc.q, version);
			final List<T> out = new ArrayList<>();
			q.apply(scope, input, out::add);
			assertThat(out).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(expectedOut);

			// JsonQuery.compile($.toString()).toString() === $.toString()
			final String s1 = q.toString();
			final String s2 = JsonQuery.<T>compile(s1, version).toString();
			assertThat(s2).as("inconsistent tostring: %s", command).isEqualTo(s1);

			// JsonQuery.compile($.toString()).apply(in) === $.apply(in)
			final JsonQuery<T> q1 = JsonQuery.compile(s1, version);
			final List<T> out1 = new ArrayList<>();
			q1.apply(scope, input, out1::add);
			assertThat(out1).as("bad tostring: %s", command)
					.usingElementComparator(comparator)
					.isEqualTo(expectedOut);
		} catch (final Throwable e) {
			failed = true;
			if (!tc.failing) {
				if (e instanceof AssertionError)
					throw e;
				e.addSuppressed(new RuntimeException("NOTE: " + command));
				throw e;
			}
		}

		if (tc.failing)
			assertThat(failed).describedAs("The test case is marked as failing but completed successfully").isTrue();
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void test(final String tcText) throws Throwable {
		final TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		for (final Version version : Versions.versions()) {
			if (tc.version == null || tc.version.contains(version)) {
				test(tc, version);
			}
		}
	}
}
