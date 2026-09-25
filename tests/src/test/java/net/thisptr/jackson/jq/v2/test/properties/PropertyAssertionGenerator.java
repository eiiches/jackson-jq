package net.thisptr.jackson.jq.v2.test.properties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;

public class PropertyAssertionGenerator {
	private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

	public static void main(String[] args) throws Exception {
		Path repoRoot = Path.of(args.length > 0 ? args[0] : ".");
		Path testCasesDir = repoRoot.resolve("tests/test-cases");
		if (!Files.isDirectory(testCasesDir)) {
			System.err.println("Test cases directory not found at: " + testCasesDir);
			System.exit(1);
		}

		List<Path> yamlFiles = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(testCasesDir)) {
			stream.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
					.forEach(yamlFiles::add);
		}
		Collections.sort(yamlFiles);

		System.out.printf("Found %d YAML files to process.%n", yamlFiles.size());
		@Var int totalTestCases = 0;
		@Var int updatedTestCases = 0;

		for (Path yamlFile : yamlFiles) {
			List<TestCase> testCases;
			try {
				TestCase[] array = YAML_MAPPER.readValue(yamlFile.toFile(), TestCase[].class);
				testCases = List.of(array);
			} catch (Exception e) {
				System.err.printf("Skipping %s (cannot parse as TestCase[]): %s%n", yamlFile, e.getMessage());
				continue;
			}

			@Var boolean modified = false;
			List<TestCase.PropertyAssertion> computedProperties = new ArrayList<>();

			for (TestCase tc : testCases) {
				totalTestCases++;
				if (!tc.shouldCompile) {
					computedProperties.add(null);
					continue;
				}

				Version version = selectVersion(tc);
				Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
				try {
					Environment<JsonNode> env = buildEnvironment(version, moduleSearchPath);
					JsonQuery<JsonNode> query = env.compile(tc.q);
					ExpressionProperties props = query.getProperties();

					TestCase.PropertyAssertion assertion = new TestCase.PropertyAssertion(
							props.cardinality(), props.dependsOnInput(), props.dependsOnExternalState());
					computedProperties.add(assertion);
					modified = true;
					updatedTestCases++;
				} catch (Exception e) {
					System.err.printf("Error evaluating properties for %s: '%s': %s%n", yamlFile.getFileName(), tc.q, e.getMessage());
					computedProperties.add(null);
				} finally {
					if (moduleSearchPath != null) {
						ModuleFixtures.cleanup(moduleSearchPath);
					}
				}
			}

			if (modified) {
				applyPropertiesToYaml(yamlFile, computedProperties);
			}
		}

		System.out.printf("Done! Updated %d of %d test cases across %d files.%n", updatedTestCases, totalTestCases, yamlFiles.size());
	}

	private static Version selectVersion(TestCase tc) {
		List<Version> versions = Versions.versions();
		if (tc.version == null) {
			return Versions.JQ_1_7;
		}
		for (int i = versions.size() - 1; i >= 0; i--) {
			if (tc.version.contains(versions.get(i))) {
				return versions.get(i);
			}
		}
		return Versions.JQ_1_7;
	}

	private static Environment<JsonNode> buildEnvironment(Version version, @Nullable Path moduleSearchPath) {
		EnvironmentBuilder<JsonNode> envBuilder = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), version);
		if (moduleSearchPath != null) {
			envBuilder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), moduleSearchPath))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		return envBuilder
				.defineVariable("ENV", () -> envBuilder.getJsonProvider().createObject(Collections.singletonMap("PAGER", envBuilder.getJsonProvider().createString("less"))))
				.build();
	}

	private static void applyPropertiesToYaml(Path yamlFile, List<TestCase.PropertyAssertion> computedProperties) throws IOException {
		List<String> lines = Files.readAllLines(yamlFile, StandardCharsets.UTF_8);
		List<String> newLines = new ArrayList<>();
		@Var int tcIndex = -1;
		@Var boolean inTypes = false;
		@Var boolean inProps = false;
		@Var boolean insertedForCurrent = false;

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String trimmed = line.trim();

			if (trimmed.startsWith("- q:") || trimmed.startsWith("-  q:")) {
				if (tcIndex >= 0 && !insertedForCurrent) {
					insertPropertiesIfPresent(newLines, computedProperties, tcIndex);
				}
				tcIndex++;
				inTypes = false;
				inProps = false;
				insertedForCurrent = false;
				newLines.add(line);
				continue;
			}

			if (trimmed.startsWith("types:")) {
				inTypes = true;
				newLines.add(line);
				continue;
			}

			if (inTypes) {
				if (line.startsWith("  - ") || line.startsWith("    ") || trimmed.startsWith("input:") || trimmed.startsWith("output:")) {
					newLines.add(line);
					continue;
				} else {
					inTypes = false;
					if (!insertedForCurrent) {
						insertPropertiesIfPresent(newLines, computedProperties, tcIndex);
						insertedForCurrent = true;
					}
				}
			}

			if (trimmed.startsWith("properties:")) {
				inProps = true;
				continue; // Skip old properties header
			}

			if (inProps) {
				if (line.startsWith("    cardinality:") || line.startsWith("    depends_on_input:") || line.startsWith("    depends_on_external_state:") || line.startsWith("  properties:")) {
					continue; // Skip old properties body
				} else {
					inProps = false;
				}
			}

			// If we haven't inserted properties yet and hit next field (like v:, comment:, failing:, etc.)
			if (!insertedForCurrent && (trimmed.startsWith("v:") || trimmed.startsWith("failing:") || trimmed.startsWith("comment:") || trimmed.startsWith("justification:") || trimmed.startsWith("modules:") || trimmed.startsWith("should_compile:"))) {
				insertPropertiesIfPresent(newLines, computedProperties, tcIndex);
				insertedForCurrent = true;
			}

			newLines.add(line);
		}

		if (tcIndex >= 0 && !insertedForCurrent) {
			insertPropertiesIfPresent(newLines, computedProperties, tcIndex);
		}

		@Var String content = String.join("\n", newLines);
		if (!content.endsWith("\n")) {
			content += "\n";
		}
		Files.writeString(yamlFile, content, StandardCharsets.UTF_8);
	}

	private static void insertPropertiesIfPresent(List<String> newLines, List<TestCase.PropertyAssertion> computedProperties, int tcIndex) {
		if (tcIndex >= 0 && tcIndex < computedProperties.size()) {
			TestCase.PropertyAssertion prop = computedProperties.get(tcIndex);
			if (prop != null) {
				newLines.add("  properties:");
				newLines.add("    cardinality: " + prop.cardinality);
				newLines.add("    depends_on_input: " + prop.dependsOnInput);
				newLines.add("    depends_on_external_state: " + prop.dependsOnExternalState);
			}
		}
	}
}
