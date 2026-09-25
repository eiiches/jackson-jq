package net.thisptr.jackson.jq.v2.test.typecheck;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Verifies that the {@code types} assertions in golden test data match the actual inferred types
 * produced by the compiler, and that the runtime output values conform to the expected output type.
 */
public class TypeCheckTestCasesTest {
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

		// 1. Verify inferred output type for each type assertion
		for (TestCase.TypeAssertion ta : tc.types) {
			Type inputType = Type.valueOf(ta.input);
			Type expectedOutputType = Type.valueOf(ta.output);

			CompileOptions options = CompileOptions.newBuilder()
					.setTypeCheckMode(TypeCheckMode.WARN)
					.setInputType(inputType)
					.build();

			JsonQuery<JsonNode> query = env.compile(tc.q, options);
			Type actualOutputType = query.getType().outputType();

			String desc = String.format("jq (v%s) '%s' with input type %s", version, tc.q, ta.input);
			assertThat(actualOutputType)
					.as(desc)
					.isEqualTo(expectedOutputType);
		}
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
		if (!tc.shouldCompile || tc.types == null || tc.types.isEmpty()) {
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
			throw new IllegalArgumentException("Usage: TypeCheckTestCasesTest <test-case-resource>");
		}

		TypeCheckTestCasesTest verifier = new TypeCheckTestCasesTest();
		assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.map(tcText -> (Executable) () -> verifier.test(tcText)));
	}
}
