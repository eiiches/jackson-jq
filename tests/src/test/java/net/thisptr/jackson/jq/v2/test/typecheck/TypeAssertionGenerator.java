package net.thisptr.jackson.jq.v2.test.typecheck;

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

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.internal.typecheck.ConstantTypes;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;

public class TypeAssertionGenerator {
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
			List<List<TestCase.TypeAssertion>> computedTypes = new ArrayList<>();

			for (TestCase tc : testCases) {
				totalTestCases++;
				if (!tc.shouldCompile) {
					computedTypes.add(null);
					continue;
				}

				Version version = selectVersion(tc);
				Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
				try {
					Environment<JsonNode> env = buildEnvironment(version, moduleSearchPath);

					// 1. Any input type
					Type inputType1 = AnyType.getInstance();
					CompileOptions options1 = CompileOptions.newBuilder()
							.setTypeCheckMode(TypeCheckMode.WARN)
							.setInputType(inputType1)
							.build();
					JsonQuery<JsonNode> q1 = env.compile(tc.q, options1);
					String outType1 = q1.getType().outputType().toString();

					// 2. Concrete input type
					Type inputType2 = tc.in == null || tc.in.isNull()
							? NullType.getInstance()
							: ConstantTypes.shapeOf(Jackson2JsonProvider.getInstance(), tc.in);
					CompileOptions options2 = CompileOptions.newBuilder()
							.setTypeCheckMode(TypeCheckMode.WARN)
							.setInputType(inputType2)
							.build();
					JsonQuery<JsonNode> q2 = env.compile(tc.q, options2);
					String inType2 = inputType2.toString();
					String outType2 = q2.getType().outputType().toString();

					List<TestCase.TypeAssertion> assertions = List.of(
							new TestCase.TypeAssertion("ANY", outType1),
							new TestCase.TypeAssertion(inType2, outType2));
					computedTypes.add(assertions);
					modified = true;
					updatedTestCases++;
				} catch (Exception e) {
					System.err.printf("Error evaluating types for %s: '%s': %s%n", yamlFile.getFileName(), tc.q, e.getMessage());
					computedTypes.add(null);
				} finally {
					if (moduleSearchPath != null) {
						ModuleFixtures.cleanup(moduleSearchPath);
					}
				}
			}

			if (modified) {
				applyTypesToYaml(yamlFile, computedTypes);
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

	private static void applyTypesToYaml(Path yamlFile, List<List<TestCase.TypeAssertion>> computedTypes) throws IOException {
		List<String> lines = Files.readAllLines(yamlFile, StandardCharsets.UTF_8);
		List<String> newLines = new ArrayList<>();
		@Var int tcIndex = -1;
		@Var boolean inOut = false;
		@Var boolean inTypes = false;

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String trimmed = line.trim();

			if (trimmed.startsWith("- q:") || trimmed.startsWith("-  q:")) {
				tcIndex++;
				inOut = false;
				inTypes = false;
				newLines.add(line);
				continue;
			}

			if (trimmed.startsWith("out:") || trimmed.startsWith("out :")) {
				inOut = true;
				newLines.add(line);
				// If out is flow-style e.g. "out: []" or "out: [1, 2]"
				if (trimmed.endsWith("]") || trimmed.endsWith("}")) {
					inOut = false;
					insertTypesIfPresent(newLines, computedTypes, tcIndex);
				}
				continue;
			}

			if (inOut) {
				// We are inside out block (sequence elements like "  - ...")
				// Check if the current line is still part of out
				if (line.startsWith("  - ") || line.startsWith("    ") || line.startsWith("  [") || line.startsWith("  {") || line.startsWith("  \"") || line.startsWith("  '") || line.startsWith("  null") || line.startsWith("  true") || line.startsWith("  false") || (trimmed.startsWith("-") && line.startsWith("  "))) {
					newLines.add(line);
					continue;
				} else {
					// out block ended
					inOut = false;
					insertTypesIfPresent(newLines, computedTypes, tcIndex);
				}
			}

			if (trimmed.startsWith("types:")) {
				inTypes = true;
				continue; // Skip old types line
			}

			if (inTypes) {
				if (line.startsWith("  - ") || line.startsWith("    ") || line.startsWith("  types:") || trimmed.startsWith("input:") || trimmed.startsWith("output:")) {
					continue; // Skip old types content
				} else {
					inTypes = false;
				}
			}

			newLines.add(line);
		}

		if (inOut) {
			insertTypesIfPresent(newLines, computedTypes, tcIndex);
		}

		@Var String content = String.join("\n", newLines);
		if (!content.endsWith("\n")) {
			content += "\n";
		}
		Files.writeString(yamlFile, content, StandardCharsets.UTF_8);
	}

	private static void insertTypesIfPresent(List<String> newLines, List<List<TestCase.TypeAssertion>> computedTypes, int tcIndex) {
		if (tcIndex >= 0 && tcIndex < computedTypes.size()) {
			List<TestCase.TypeAssertion> assertions = computedTypes.get(tcIndex);
			if (assertions != null && !assertions.isEmpty()) {
				newLines.add("  types:");
				for (TestCase.TypeAssertion ta : assertions) {
					newLines.add("  - input: '" + escapeSingleQuotes(ta.input) + "'");
					newLines.add("    output: '" + escapeSingleQuotes(ta.output) + "'");
				}
			}
		}
	}

	private static String escapeSingleQuotes(String s) {
		return s.replace("'", "''");
	}
}
