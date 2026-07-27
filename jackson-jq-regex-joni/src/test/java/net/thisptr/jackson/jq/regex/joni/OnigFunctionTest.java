package net.thisptr.jackson.jq.regex.joni;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.VersionRange;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.VersionRangeDeserializer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class OnigFunctionTest {
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

	private static List<TestCase> loadTestCases(final String resourcePath) throws IOException {
		try (InputStream in = OnigFunctionTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null)
				throw new IOException("Resource not found: " + resourcePath);
			final TestCase[] result = YAML_MAPPER.readValue(in, TestCase[].class);
			for (final TestCase tc : result) {
				tc.file = resourcePath;
			}
			return Arrays.asList(result);
		}
	}

	private static List<TestCase> loadJsonTestCases(final String resourcePath) throws IOException {
		try (InputStream in = OnigFunctionTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null)
				throw new IOException("Resource not found: " + resourcePath);
			final TestCase[] result = JSON_MAPPER.readValue(in, TestCase[].class);
			for (final TestCase tc : result) {
				tc.file = resourcePath;
			}
			return Arrays.asList(result);
		}
	}

	static Stream<String> testCases() throws IOException {
		final List<TestCase> testCases = new ArrayList<>();
		testCases.addAll(loadTestCases("tests/jq-1.5-onig.yaml"));
		testCases.addAll(loadTestCases("tests/jq-1.6-onig.yaml"));
		testCases.addAll(loadTestCases("tests/jq-1.5-manual-onig.yaml"));
		testCases.addAll(loadTestCases("tests/jq-1.6-manual-onig.yaml"));
		testCases.addAll(loadJsonTestCases("jq-test-extra-onig-ok.json"));

		return testCases.stream().map(a -> {
			try {
				return JSON_MAPPER.writeValueAsString(a);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void test(final String tcText) throws Throwable {
		final TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		for (final Version version : Versions.versions()) {
			if (tc.version == null || tc.version.contains(version)) {
				test(tc, version);
			}
		}
	}

	private void test(final TestCase tc, final Version version) throws Throwable {
		final Scope scope = DefaultRootScope.getInstance(version);

		final Comparator<JsonNode> comparator = tc.ignoreFieldOrder
				? new JsonNodeComparator()
				: new StrictFieldOrderComparator();

		final JsonQuery q = JsonQuery.compile(tc.q, version);
		final List<JsonNode> out = new ArrayList<>();
		q.apply(scope, tc.in, out::add);
		assertThat(out).as("jq '%s' <<< '%s'", tc.q, tc.in)
				.usingElementComparator(comparator)
				.isEqualTo(tc.out);
	}

	private static class StrictFieldOrderComparator extends JsonNodeComparator {
		private static final long serialVersionUID = 1L;

		@Override
		protected int compareObjectNode(final JsonNode o1, final JsonNode o2) {
			final Iterator<Entry<String, JsonNode>> it1 = o1.fields();
			final Iterator<Entry<String, JsonNode>> it2 = o2.fields();
			while (it1.hasNext() && it2.hasNext()) {
				final Entry<String, JsonNode> entry1 = it1.next();
				final Entry<String, JsonNode> entry2 = it2.next();
				final int r0 = entry1.getKey().compareTo(entry2.getKey());
				if (r0 != 0) return r0;
				final int r1 = compare(entry1.getValue(), entry2.getValue());
				if (r1 != 0) return r1;
			}
			return Integer.compare(o1.size(), o2.size());
		}
	}
}
