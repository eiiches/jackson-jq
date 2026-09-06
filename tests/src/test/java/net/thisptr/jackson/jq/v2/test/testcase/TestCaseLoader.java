package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class TestCaseLoader {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

	private static List<TestCase> loadTestCases(String resourceName, InputStream in) throws IOException {
		TestCase[] result;
		if (resourceName.endsWith(".yaml")) {
			result = YAML_MAPPER.readValue(in, TestCase[].class);
		} else if (resourceName.endsWith(".json")) {
			result = JSON_MAPPER.readValue(in, TestCase[].class);
		} else {
			throw new IllegalArgumentException("unsupported file format");
		}
		for (TestCase tc : result) {
			tc.file = resourceName;
		}
		return Arrays.asList(result);
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
