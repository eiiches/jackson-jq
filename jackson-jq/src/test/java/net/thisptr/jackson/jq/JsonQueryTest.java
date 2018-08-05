package net.thisptr.jackson.jq;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class JsonQueryTest {
	private static final Logger LOG = LoggerFactory.getLogger(JsonQueryTest.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

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

		@JsonProperty("known_to_fail")
		public boolean knownToFail = false;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s", q, in, out);
		}
	}

	public static List<TestCase> loadTestCases(final String resourceName, final boolean ignoreFailure) throws Throwable {
		final TestCase[] result;
		try (InputStream in = JsonQueryTest.class.getClassLoader().getResourceAsStream(resourceName)) {
			result = MAPPER.readValue(in, TestCase[].class);
			for (final TestCase tc : result) {
				tc.knownToFail = ignoreFailure;
				tc.file = resourceName;
			}
		}
		return Arrays.asList(result);
	}

	static Stream<String> defaultTestCases() throws Throwable {
		final List<TestCase> testCases = new ArrayList<>();
		testCases.addAll(loadTestCases("jq-test-manual-ok.json", false));
		testCases.addAll(loadTestCases("jq-test-manual-ng.json", true));
		testCases.addAll(loadTestCases("jq-test-all-ok.json", false));
		testCases.addAll(loadTestCases("jq-test-all-ng.json", true));
		testCases.addAll(loadTestCases("jq-test-extra-ok.json", false));
		testCases.addAll(loadTestCases("jq-test-onig-ok.json", false));
		testCases.addAll(loadTestCases("jq-test-onig-ng.json", true));
		return testCases.stream().map(a -> {
			try {
				return MAPPER.writeValueAsString(a);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("defaultTestCases")
	public void test(final String tcText) throws Throwable {
		final TestCase tc = MAPPER.readValue(tcText, TestCase.class);
		try {
			LOG.info("Running test ({}): {}", tc.file, tc.toString().replace('\n', ' '));
			final JsonQuery q = JsonQuery.compile(tc.q);
			final String s1 = q.toString();

			try {
				final String s2 = JsonQuery.compile(s1).toString();
				assertEquals(s1, s2);
			} catch (Throwable e) {
				LOG.error(" * toString() generated unparsable or incosistent query: {}", s1);
				throw e;
			}

			final List<JsonNode> actual = q.apply(DefaultRootScope.getInstance(), tc.in);
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
			final JsonQuery q2 = JsonQuery.compile(s1);
			final List<JsonNode> actual2 = q2.apply(DefaultRootScope.getInstance(), tc.in);
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
			LOG.error(" * Failed with: {}", e.getMessage());
			if (!tc.knownToFail)
				throw e;
		}
	}
}
