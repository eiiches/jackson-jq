package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class TestCaseLoader {
	private static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
			.build();
	private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
			.build();

	static List<TestCase> loadTestCases(String resourceName, InputStream in) throws IOException {
		return loadTestCases(resourceName, in, true);
	}

	static List<TestCase> loadTestCasesUnchecked(String resourceName, InputStream in) throws IOException {
		return loadTestCases(resourceName, in, false);
	}

	private static List<TestCase> loadTestCases(String resourceName, InputStream in, boolean validate) throws IOException {
		ObjectMapper mapper;
		if (resourceName.endsWith(".yaml"))
			mapper = YAML_MAPPER;
		else if (resourceName.endsWith(".json"))
			mapper = JSON_MAPPER;
		else
			throw new IllegalArgumentException("unsupported file format");
		JsonNode root = mapper.readTree(in);
		if (root == null || !root.isArray())
			throw new IllegalArgumentException("test case resource must be an array: " + resourceName);
		List<TestCase> result = new ArrayList<>();
		for (JsonNode node : root) {
			TestCase tc = mapper.treeToValue(node, TestCase.class);
			tc.file = resourceName;
			// The position is worth naming as well as the command: a file may write the same query
			// and input more than once, and then nothing else tells the two cases apart.
			if (validate)
				validate(tc, String.format("%s case %d: %s", resourceName, result.size() + 1, tc));
			result.add(tc);
		}
		return List.copyOf(result);
	}

	public static TestCase parseTestCase(String json) throws IOException {
		JsonNode node = JSON_MAPPER.readTree(json);
		TestCase tc = JSON_MAPPER.treeToValue(node, TestCase.class);
		validate(tc, tc.describe());
		return tc;
	}

	/**
	 * Validates one case, naming it in whatever it rejects. The checks themselves only report what
	 * is wrong, so without this the reason reaches the reader with no case attached.
	 */
	private static void validate(TestCase tc, String where) {
		try {
			tc.validateTypes();
			tc.validateProperties();
			tc.expectations.validate();
		} catch (IllegalArgumentException failure) {
			throw new IllegalArgumentException(where + ": " + failure.getMessage(), failure);
		}
	}

	public static Stream<String> loadTestCasesAsJsonStrings(String resourceName) throws IOException {
		ClassLoader classLoader = TestCaseLoader.class.getClassLoader();
		try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
			if (in == null)
				throw new IOException("Failed to load " + resourceName);
			return loadTestCases(resourceName, in).stream().map(tc -> {
				try {
					return JSON_MAPPER.writeValueAsString(tc);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

}
