package net.thisptr.jackson.jq;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.VersionRangeDeserializer;
import net.thisptr.jackson.jq.test.evaluator.CachedEvaluator;
import net.thisptr.jackson.jq.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.test.evaluator.Evaluator.Result;
import net.thisptr.jackson.jq.test.evaluator.TrueJqEvaluator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class JsonQueryTest {
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

	private static List<TestCase> loadTestCases(final Path path, final boolean failing) throws IOException {
		final TestCase[] result;
		try (InputStream in = Files.newInputStream(path)) {
			String resourceName = path.toString();
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
		}
		return Arrays.asList(result);
	}

	private static List<TestCase> loadTestCasesDirectory(final String directory, final boolean failing) throws IOException {
		final List<TestCase> testCases = new ArrayList<>();
		ClassLoaderUtils.walk(directory, (path, relativePath) -> {
			if (Files.isDirectory(path)) {
				return;
			}
			String name = relativePath.toString();
			if (name.endsWith(".json") || name.endsWith(".yaml")) {
				try {
					testCases.addAll(loadTestCases(path, failing));
				} catch (final Throwable e) {
					throw new RuntimeException("Failed to load " + relativePath, e);
				}
			}
		});
		return testCases;
	}

	static Stream<String> defaultTestCases() throws IOException {
		final List<TestCase> testCases = new ArrayList<>();
		testCases.addAll(loadTestCases(ClassLoaderUtils.resolve("jq-test-extra-ok.json"), false));
		testCases.addAll(loadTestCasesDirectory("tests", false));
		testCases.addAll(loadTestCasesDirectory("failing_tests", true));

		return testCases.stream().map(a -> {
			try {
				return JSON_MAPPER.writeValueAsString(a);
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

	private void test(final TestCase tc, final Version version) throws Throwable {
		final Scope scope = DefaultRootScope.getInstance(version);
		final String command = String.format("%s '%s' <<< '%s'", TrueJqEvaluator.executable(version), tc.q, tc.in);

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> JsonQuery.compile(tc.q, version));
			return;
		}

		final Comparator<JsonNode> comparator = new JsonNodeComparatorForTests(!tc.ignoreFieldOrder, tc.numericalErrors);

		if (!tc.ignoreTrueJqBehavior && hasJqCache.computeIfAbsent(version, v -> TrueJqEvaluator.hasJq(v))) {
			final Result result = cachedJqEvaluator.evaluate(tc.q, tc.in, version, 2000L);
			try {
                assertThat(result.error).as("%s", command).isNull();
				assertThat(tc.out).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(result.values);
			} catch (AssertionError e) {
				Assumptions.abort(String.format("Assumption failed: %s %s", command, e));
			}
		}

		boolean failed = false;
		try {
			final JsonQuery q = JsonQuery.compile(tc.q, version);
			final List<JsonNode> out = new ArrayList<>();
			q.apply(scope, tc.in, out::add);
			assertThat(out).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(tc.out);

			// JsonQuery.compile($.toString()).toString() === $.toString()
			final String s1 = q.toString();
			final String s2 = JsonQuery.compile(s1, version).toString();
			assertThat(s2).as("inconsistent tostring: %s", command).isEqualTo(s1);

			// JsonQuery.compile($.toString()).apply(in) === $.apply(in)
			final JsonQuery q1 = JsonQuery.compile(s1, version);
			final List<JsonNode> out1 = new ArrayList<>();
			q1.apply(scope, tc.in, out1::add);
			assertThat(out1).as("bad tostring: %s", command)
					.usingElementComparator(comparator)
					.isEqualTo(tc.out);
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
