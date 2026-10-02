package net.thisptr.jackson.jq.v2.test.typecheck;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
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
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
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
	private void testVersion(TestCase tc, List<TestCase.TypeAssertion> rows, Version version, @Nullable Path moduleSearchPath) {
		EnvironmentBuilder<JsonNode> envBuilder = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), version);
		if (moduleSearchPath != null) {
			envBuilder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), moduleSearchPath))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		Environment<JsonNode> env = envBuilder
				// Regex is an extension module; the suite includes it because jq's test cases call
				// test, match, sub and the rest by their bare names.
				.includeModule(new JoniRegexModule())
				.defineVariable("ENV", () -> envBuilder.getJsonProvider().createObject(Collections.singletonMap("PAGER", envBuilder.getJsonProvider().createString("less"))))
				.build();

		// One environment per version; the rows of that version differ only in the input type they
		// compile the query with.
		for (TestCase.TypeAssertion ta : rows) {
			CompileOptions options = CompileOptions.newBuilder()
					.setTypeCheckMode(TypeCheckMode.WARN)
					.setInputType(Type.valueOf(ta.input))
					.build();

			JsonQuery<JsonNode> query = env.compile(tc.q, options);
			Type actualOutputType = query.getType().outputType();

			String desc = String.format("jq (v%s) '%s' with input type %s", version, tc.q, ta.input);
			assertThat(actualOutputType)
					.as(desc)
					.isEqualTo(Type.valueOf(ta.output));
		}
	}

	/**
	 * The rows that claim something about {@code version}.
	 */
	private static List<TestCase.TypeAssertion> rowsFor(TestCase tc, Version version) {
		if (!tc.appliesToAssertions(version))
			return List.of();
		List<TestCase.TypeAssertion> rows = new ArrayList<>();
		for (TestCase.TypeAssertion ta : tc.types) {
			if (ta.appliesTo(version))
				rows.add(ta);
		}
		return rows;
	}

	public void test(String tcText) throws Throwable {
		TestCase tc = TestCaseLoader.parseTestCase(tcText);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			if (tc.types.isEmpty()) {
				if (TypeAssertionGenerator.assertionsFor(tc, moduleSearchPath).isEmpty())
					return;
				throw new AssertionError(String.format("Missing types for jq '%s' in %s", tc.q, tc.file));
			}
			// Checking a row on every version it covers only reports the versions a row names, so a row
			// that names none has to be caught here rather than going unchecked.
			for (TestCase.TypeAssertion ta : tc.types) {
				if (Versions.versions().stream().noneMatch(version -> ta.appliesTo(version) && tc.appliesToAssertions(version)))
					throw new IllegalArgumentException("type assertion and case ranges match no configured version: " + tc.q);
			}

			List<Executable> checks = new ArrayList<>();
			for (Version version : Versions.versions()) {
				List<TestCase.TypeAssertion> rows = rowsFor(tc, version);
				if (!rows.isEmpty())
					checks.add(() -> testVersion(tc, rows, version, moduleSearchPath));
			}
			assertAll(tc.q, checks);
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
