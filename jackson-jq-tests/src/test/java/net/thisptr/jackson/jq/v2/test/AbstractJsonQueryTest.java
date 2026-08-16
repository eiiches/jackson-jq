package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.test.evaluator.CachedEvaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator.Result;
import net.thisptr.jackson.jq.v2.test.evaluator.TrueJqEvaluator;

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
	private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TestCase {
		@JsonProperty("q")
		public String q = "";

		@JsonProperty("in")
		public JsonNode in = NullNode.getInstance();

		@JsonProperty("out")
		public List<JsonNode> out = Collections.emptyList();

		@JsonProperty("file")
		public String file = "";

		@JsonProperty("failing")
		public @Nullable Boolean failing;

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
		public @Nullable VersionRange version;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", q, in, out, version != null ? version : "any");
		}
	}

	/**
	 * Create an Environment for the given version.
	 *
	 * @param version The jq version to use
	 * @return A configured environment ready for query compilation
	 */
	protected abstract Environment<T> createEnvironment(Version version);

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

	private static List<TestCase> loadTestCases(String resourceName, InputStream in, boolean failing) throws IOException {
		TestCase[] result;
		if (resourceName.endsWith(".yaml")) {
			result = YAML_MAPPER.readValue(in, TestCase[].class);
		} else if (resourceName.endsWith(".json")) {
			result = JSON_MAPPER.readValue(in, TestCase[].class);
		} else {
			throw new IllegalArgumentException("unsupported file format");
		}
		for (TestCase tc : result) {
			if (tc.failing == null)
				tc.failing = failing;
			tc.file = resourceName;
		}
		return Arrays.asList(result);
	}

	protected static Stream<String> defaultTestCases(ClassLoader classLoader) throws IOException {
		List<String> resourceNames = ClassLoaderUtils.listResources(classLoader, "tests").stream()
				.filter(name -> name.endsWith(".json") || name.endsWith(".yaml"))
				.sorted()
				.collect(Collectors.toList());
		if (resourceNames.isEmpty())
			throw new IllegalStateException("No test cases found under classpath resource tests/");

		List<TestCase> testCases = new ArrayList<>();
		for (String resourceName : resourceNames) {
			try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
				if (in == null)
					throw new IOException("Failed to load " + resourceName);
				testCases.addAll(loadTestCases(resourceName, in, false));
			}
		}

		return testCases.stream().map(tc -> {
			try {
				return JSON_MAPPER.writeValueAsString(tc);
			} catch (IOException e) {
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

	private void test(TestCase tc, Version version) throws Throwable {
		Environment<T> env = createEnvironment(version);
		env.addVariable("ENV", () -> {
			T envObj = env.jsonProvider().createObject();
			env.jsonProvider().set(envObj, "PAGER", env.jsonProvider().createString("less"));
			return envObj;
		});

		String command = String.format("%s '%s' <<< '%s'", TrueJqEvaluator.executable(version), tc.q, tc.in);

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

		if (!tc.ignoreTrueJqBehavior && hasJqCache.computeIfAbsent(version, v -> TrueJqEvaluator.hasJq(v))) {
			Result result = cachedJqEvaluator.evaluate(tc.q, tc.in, version, 2000L);
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
	public void test(String tcText) throws Throwable {
		TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		for (Version version : Versions.versions()) {
			if (tc.version == null || tc.version.contains(version)) {
				test(tc, version);
			}
		}
	}
}
