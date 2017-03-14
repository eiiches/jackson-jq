package net.thisptr.jackson.jq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

@RunWith(Enclosed.class)
public class JsonQueryTest {
	private static Logger log = LoggerFactory.getLogger(JsonQueryTest.class);

	public static class Simple {
		private static ObjectMapper mapper = new ObjectMapper();

		@Test
		public void testMulti() throws JsonQueryException, JsonProcessingException, IOException {
			final JsonQuery q = JsonQuery.compile("[.[] + 1]");
			final List<JsonNode> r = q.apply(Arrays.asList(mapper.readTree("[1, 2]"), mapper.readTree("[3, 4]")));
			assertEquals(mapper.readTree("[2,3]"), r.get(0));
			assertEquals(mapper.readTree("[4,5]"), r.get(1));
		}
	}

	@Ignore
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TestCase {
		@JsonProperty("q")
		public String q;
		@JsonProperty("in")
		public JsonNode in;
		@JsonProperty("out")
		public List<JsonNode> out;

		public String source;
		public boolean ignoreFailure = false;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s", q, in, out);
		}
	}

	@RunWith(Theories.class)
	public static class JsonTestCases {
		public static TestCase[] loadTestCases(final String resourceName, final boolean ignoreFailure) {
			try {
				final ObjectMapper mapper = new ObjectMapper();
				try (final InputStream in = JsonTestCases.class.getClassLoader().getResourceAsStream(resourceName)) {
					final TestCase[] result = mapper.readValue(in, TestCase[].class);
					for (final TestCase tc : result) {
						tc.ignoreFailure = ignoreFailure;
						tc.source = resourceName;
					}
					return result;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@DataPoints
		public static TestCase[] TESTCASES_MANUAL = loadTestCases("jq-test-manual-ok.json", false);

		@DataPoints
		public static TestCase[] TESTCASES_ONIG = loadTestCases("jq-test-onig-ok.json", false);

		@DataPoints
		public static TestCase[] TESTCASES_ALL = loadTestCases("jq-test-all-ok.json", false);

		@DataPoints
		public static TestCase[] TESTCASES_EXTRA = loadTestCases("jq-test-extra-ok.json", false);

		@DataPoints
		public static TestCase[] TESTCASES_ALL_FAIL = loadTestCases("jq-test-all-ng.json", true);

		@DataPoints
		public static TestCase[] TESTCASES_MANUAL_FAIL = loadTestCases("jq-test-manual-ng.json", true);

		@DataPoints
		public static TestCase[] TESTCASES_ONIG_FAIL = loadTestCases("jq-test-onig-ng.json", true);

		@Theory
		public void test(final TestCase tc) throws Throwable {
			try {
				log.info("Running test ({}): {}", tc.source, tc.toString().replace('\n', ' '));
				final JsonQuery q = JsonQuery.compile(tc.q);
				final String s1 = q.toString();

				try {
					final String s2 = JsonQuery.compile(s1).toString();
					assertEquals(s1, s2);
				} catch (Throwable e) {
					log.error(" * toString() generated unparsable or incosistent query: {}", s1);
					throw e;
				}

				final List<JsonNode> actual = q.apply(tc.in);
				final List<JsonNode> expected = tc.out;

				if (actual.size() != expected.size()) {
					log.error(" * {} (actual) != {} (expected)", actual, expected);
					log.error(" * Expected length of {}, but got {}.", expected.size(), actual.size());
				}
				assertEquals(actual.size(), expected.size());
				for (int i = 0; i < actual.size(); ++i) {
					final int r = new JsonNodeComparator().compare(actual.get(i), expected.get(i));
					if (r != 0) {
						log.error(" * {} (actual) != {} (expected)", actual, expected);
						log.error(" * The {}'th element does not match: {} (actual) != {} (expected).", i, actual.get(i), expected.get(i));
					}
					assertTrue("0", r == 0);
				}

				// JsonQuery.compile($.toString()).apply(in) === $.apply(in)
				final JsonQuery q2 = JsonQuery.compile(s1);
				final List<JsonNode> actual2 = q2.apply(tc.in);
				if (actual.size() != actual2.size()) {
					log.error(" * The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: {} != {} (original)", s1, tc.q);
					log.error(" * {} (actual) != {} (actual2)", actual, actual2);
					log.error(" * Expected length of {}, but got {}.", actual.size(), actual2.size());
				}
				assertEquals("The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: ", actual.size(), actual2.size());
				for (int i = 0; i < actual.size(); ++i) {
					final int r = new JsonNodeComparator().compare(actual.get(i), actual2.get(i));
					if (r != 0) {
						log.error(" * The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: {} != {} (original)", s1, tc.q);
						log.error(" * {} (actual) != {} (actual2)", actual, actual2);
						log.error(" * The {}'th element does not match: {} (actual) != {} (actual2).", i, actual.get(i), actual2.get(i));
					}
					assertTrue("The contract JsonQuery.compile($.toString()).apply(in) != $.apply(in) violated: ", r == 0);
				}
			} catch (final Throwable e) {
				log.error(" * Failed with: {}", e.getMessage());
				if (!tc.ignoreFailure)
					throw e;
			}
		}
	}
}
