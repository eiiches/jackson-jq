package net.thisptr.jackson.jq.v2.test.properties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
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
	private void testVersion(TestCase tc, TestCase.PropertyAssertion expected, Version version, @Nullable Path moduleSearchPath) throws Throwable {
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

		JsonQuery<JsonNode> query = env.compile(tc.q);
		ExpressionProperties actual = query.getProperties();

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

		TestCase.AbstractExpectation expectationRow = tc.expectations.resolve(version, false, System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
		if (expectationRow.runtimeError == null && expectationRow.compileError == null
				&& !expectationRow.limitExceeded() && !expectationRow.timedOut() && !expectationRow.unstable()) {
			List<JsonNode> actualOutputs = new ArrayList<>();
			query.apply(tc.input, actualOutputs::add);
			if (actual.cardinality() == Cardinality.ZERO) {
				assertThat(actualOutputs)
						.as("cardinality of %s is ZERO, but actual output count is %d", desc, actualOutputs.size())
						.isEmpty();
			} else if (actual.cardinality() == Cardinality.ONE) {
				assertThat(actualOutputs)
						.as("cardinality of %s is ONE, but actual output count is %d", desc, actualOutputs.size())
						.hasSize(1);
			} else if (actualOutputs.size() > 1) {
				assertThat(actual.cardinality())
						.as("actual output count is %d (> 1), so cardinality of %s must be UNKNOWN", actualOutputs.size(), desc)
						.isEqualTo(Cardinality.UNKNOWN);
			}

			if (!actual.dependsOnInput()) {
				JsonNode[] alternatives = new JsonNode[] {
						env.getJsonProvider().createNull(),
						env.getJsonProvider().createBoolean(true),
						env.getJsonProvider().createNumber(12345),
						env.getJsonProvider().createString("alt-input"),
						env.getJsonProvider().createArray(List.of()),
						env.getJsonProvider().createObject(Collections.emptyMap()),
				};
				for (JsonNode alt : alternatives) {
					List<JsonNode> altOutputs = new ArrayList<>();
					try {
						query.apply(alt, altOutputs::add);
						assertThat(altOutputs)
								.as("actual outputs of %s on alternative input %s must equal outputs on %s when depends_on_input is false", desc, alt, tc.input)
								.isEqualTo(actualOutputs);
					} catch (Exception e) {
						throw new AssertionError(String.format("Query %s marked depends_on_input=false threw exception on alternative input %s", desc, alt), e);
					}
				}
			}
		}
	}

	public void test(String tcText) throws Throwable {
		TestCase tc = TestCaseLoader.parseTestCase(tcText);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			if (tc.properties.isEmpty()) {
				if (PropertyAssertionGenerator.assertionsFor(tc, moduleSearchPath).isEmpty())
					return;
				throw new AssertionError(String.format("Missing properties for jq '%s' in %s", tc.q, tc.file));
			}
			for (TestCase.PropertyAssertion row : tc.properties) {
				if (Versions.versions().stream().noneMatch(version -> row.appliesTo(version) && tc.appliesToAssertions(version)))
					throw new IllegalArgumentException("property assertion and case ranges match no configured version: " + tc.q);
			}

			List<Executable> checks = new ArrayList<>();
			for (Version version : Versions.versions()) {
				if (!tc.appliesToAssertions(version))
					continue;
				for (TestCase.PropertyAssertion row : tc.properties) {
					if (row.appliesTo(version)) {
						checks.add(() -> testVersion(tc, row, version, moduleSearchPath));
						break;
					}
				}
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
			throw new IllegalArgumentException("Usage: PropertyCheckTestCasesTest <test-case-resource>");
		}

		PropertyCheckTestCasesTest verifier = new PropertyCheckTestCasesTest();
		assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.map(tcText -> (Executable) () -> verifier.test(tcText)));
	}
}
