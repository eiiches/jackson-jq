package net.thisptr.jackson.jq.v2.test.properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

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
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Verifies that the {@code properties} assertions in golden test data match the actual properties
 * inferred by the compiler.
 */
public class PropertyCheckTestCasesTest {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private void testVersion(TestCase tc, Version version, @Nullable Path moduleSearchPath) {
		EnvironmentBuilder<JsonNode> envBuilder = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), version);
		if (moduleSearchPath != null) {
			envBuilder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), moduleSearchPath))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		Environment<JsonNode> env = envBuilder
				.defineVariable("ENV", () -> envBuilder.getJsonProvider().createObject(Collections.singletonMap("PAGER", envBuilder.getJsonProvider().createString("less"))))
				.build();

		JsonQuery<JsonNode> query = env.compile(tc.q);
		ExpressionProperties actual = query.getProperties();

		TestCase.PropertyAssertion expected = tc.properties;
		if (expected == null) {
			return;
		}

		String desc = String.format("jq (v%s) '%s'", version, tc.q);
		assertThat(actual.cardinality())
				.as("cardinality of %s", desc)
				.isEqualTo(expected.cardinality);
		assertThat(actual.dependsOnInput())
				.as("depends_on_input of %s", desc)
				.isEqualTo(expected.dependsOnInput);
		assertThat(actual.dependsOnExternalState())
				.as("depends_on_external_state of %s", desc)
				.isEqualTo(expected.dependsOnExternalState);
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

	public void test(String tcText) throws Throwable {
		TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		if (!tc.shouldCompile || tc.properties == null) {
			return;
		}

		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			Version version = selectVersion(tc);
			testVersion(tc, version, moduleSearchPath);
		} finally {
			if (moduleSearchPath != null) {
				ModuleFixtures.cleanup(moduleSearchPath);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Usage: PropertyCheckTestCasesTest <test-case-resource>");
		}

		PropertyCheckTestCasesTest verifier = new PropertyCheckTestCasesTest();
		assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.map(tcText -> (Executable) () -> verifier.test(tcText)));
	}
}
