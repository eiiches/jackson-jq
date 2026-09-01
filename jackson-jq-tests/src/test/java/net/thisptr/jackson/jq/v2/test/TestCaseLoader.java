package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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

	public static Stream<String> loadAllTestCasesAsJsonStrings() throws IOException {
		return loadAllTestCasesAsJsonStrings(TestCaseLoader.class.getClassLoader());
	}

	public static Stream<String> loadAllTestCasesAsJsonStrings(ClassLoader classLoader) throws IOException {
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
				testCases.addAll(loadTestCases(resourceName, in));
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
}
