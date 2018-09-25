package net.thisptr.jackson.jq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.VersionRangeDeserializer;
import net.thisptr.jackson.jq.test.evaluator.TrueJqEvaluator;
import net.thisptr.jackson.jq.test.misc.ComparableJsonNode;

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

	private static List<TestCase> loadTestCases(final ClassLoader classLoader, final String resourceName, final boolean failing) throws Throwable {
		final TestCase[] result;
		try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
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

	private static List<TestCase> loadTestCasesDirectory(final ClassLoader classLoader, final String directory, final boolean failing) throws Throwable {
		final List<TestCase> testCases = new ArrayList<>();
		final ClassPath classPath = ClassPath.from(classLoader);
		for (final ResourceInfo resource : classPath.getResources()) {
			final String name = resource.getResourceName();
			if (!name.startsWith(directory))
				continue;
			if (!name.endsWith(".json") && !name.endsWith(".yaml"))
				continue;
			try {
				testCases.addAll(loadTestCases(classLoader, name, failing));
			} catch (final Throwable e) {
				throw new RuntimeException("Failed to load " + name, e);
			}
		}
		return testCases;
	}

	static Stream<String> defaultTestCases() throws Throwable {
		final ClassLoader classLoader = JsonQueryTest.class.getClassLoader();

		final List<TestCase> testCases = new ArrayList<>();
		testCases.addAll(loadTestCases(classLoader, "jq-test-manual-ok.json", false));
		testCases.addAll(loadTestCases(classLoader, "jq-test-manual-ng.json", true));
		testCases.addAll(loadTestCases(classLoader, "jq-test-all-ok.json", false));
		testCases.addAll(loadTestCases(classLoader, "jq-test-all-ng.json", true));
		testCases.addAll(loadTestCases(classLoader, "jq-test-extra-ok.json", false));
		testCases.addAll(loadTestCases(classLoader, "jq-test-onig-ok.json", false));
		testCases.addAll(loadTestCases(classLoader, "jq-test-onig-ng.json", true));
		testCases.addAll(loadTestCasesDirectory(classLoader, "tests", false));
		testCases.addAll(loadTestCasesDirectory(classLoader, "failing_tests", true));

		return testCases.stream().map(a -> {
			try {
				return JSON_MAPPER.writeValueAsString(a);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static List<ComparableJsonNode> wrap(final List<JsonNode> values) {
		return ComparableJsonNode.wrap(values);
	}

	private void test(final TestCase tc, final Version version) throws Throwable {
		final Scope scope = DefaultRootScope.getInstance(version);
		final String command = String.format("%s '%s' <<< '%s'", TrueJqEvaluator.executable(version), tc.q, tc.in);

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> JsonQuery.compile(tc.q, version));
			return;
		}

		boolean failed = false;
		try {
			final JsonQuery q = JsonQuery.compile(tc.q, version);
			assertThat(wrap(q.apply(scope, tc.in))).as("%s", command).isEqualTo(wrap(tc.out));

			// JsonQuery.compile($.toString()).toString() === $.toString()
			final String s1 = q.toString();
			final String s2 = JsonQuery.compile(s1, version).toString();
			assertThat(s2).as("inconsistent tostring: %s", command).isEqualTo(s1);

			// JsonQuery.compile($.toString()).apply(in) === $.apply(in)
			final JsonQuery q1 = JsonQuery.compile(s1, version);
			assertThat(wrap(q1.apply(scope, tc.in))).as("bad tostring: %s", command).isEqualTo(wrap(tc.out));
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
			assertThat(failed).describedAs("unexpectedly succeeded").isTrue();
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
