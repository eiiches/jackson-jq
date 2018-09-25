package net.thisptr.jackson.jq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class JsonQueryTest {
	private static final Logger LOG = LoggerFactory.getLogger(JsonQueryTest.class);

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

		@JsonProperty("v")
		@JsonDeserialize(using = VersionRangeDeserializer.class)
		@JsonSerialize(using = ToStringSerializer.class)
		public VersionRange version;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", q, in, out, version != null ? version : "any");
		}
	}

	public static class VersionRangeDeserializer extends StdDeserializer<VersionRange> {
		private static final long serialVersionUID = -4054473248484615401L;

		public VersionRangeDeserializer() {
			super(VersionRange.class);
		}

		@Override
		public VersionRange deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final String text = p.readValueAs(String.class);
			if (text == null)
				return null;
			return VersionRange.valueOf(text);
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

	private void test(final TestCase tc, final Version version) throws Throwable {
		LOG.info("Running test ({}): {}", tc.file, tc.toString().replace('\n', ' '));

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> JsonQuery.compile(tc.q, version));
			return;
		}

		boolean failed = false;
		try {
			final JsonQuery q = JsonQuery.compile(tc.q, version);
			final String s1 = q.toString();

			try {
				final String s2 = JsonQuery.compile(s1, version).toString();
				assertEquals(s1, s2);
			} catch (Throwable e) {
				LOG.error(" * toString() generated unparsable or incosistent query: {}", s1);
				throw e;
			}

			final List<JsonNode> actual = q.apply(DefaultRootScope.getInstance(version), tc.in);
			final List<JsonNode> expected = tc.out;

			if (actual.size() != expected.size()) {
				LOG.error(" * {} (actual) != {} (expected)", actual, expected);
				LOG.error(" * Expected length of {}, but got {}.", expected.size(), actual.size());
			}
			assertEquals(actual.size(), expected.size());
			for (int i = 0; i < actual.size(); ++i) {
				final int r = new JsonNodeComparator().compare(actual.get(i), expected.get(i));
				if (r != 0) {
					LOG.error(" * {} (actual) != {} (expected)", actual, expected);
					LOG.error(" * The {}'th element does not match: {} (actual) != {} (expected).", i, actual.get(i), expected.get(i));
				}
				assertTrue(r == 0, "0");
			}

			// JsonQuery.compile($.toString()).apply(in) === $.apply(in)
			final JsonQuery q2 = JsonQuery.compile(s1, version);
			final List<JsonNode> actual2 = q2.apply(DefaultRootScope.getInstance(version), tc.in);
			if (actual.size() != actual2.size()) {
				LOG.error(" * The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: {} != {} (original)", s1, tc.q);
				LOG.error(" * {} (actual) != {} (actual2)", actual, actual2);
				LOG.error(" * Expected length of {}, but got {}.", actual.size(), actual2.size());
			}
			assertEquals(actual.size(), actual2.size(), "The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated.");
			for (int i = 0; i < actual.size(); ++i) {
				final int r = new JsonNodeComparator().compare(actual.get(i), actual2.get(i));
				if (r != 0) {
					LOG.error(" * The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: {} != {} (original)", s1, tc.q);
					LOG.error(" * {} (actual) != {} (actual2)", actual, actual2);
					LOG.error(" * The {}'th element does not match: {} (actual) != {} (actual2).", i, actual.get(i), actual2.get(i));
				}
				assertTrue(r == 0, "The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated.");
			}
		} catch (final Throwable e) {
			failed = true;
			LOG.error(" * Failed with: {}", e.getMessage());
			if (!tc.failing)
				throw e;
		}

		assertEquals(tc.failing, failed, "marked failing but succeeded");
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
