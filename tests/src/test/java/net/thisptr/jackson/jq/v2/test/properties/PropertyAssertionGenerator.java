package net.thisptr.jackson.jq.v2.test.properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

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
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFiles;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFormatter;
import net.thisptr.jackson.jq.v2.test.testcase.VersionedRows;

/**
 * Rewrites the {@code properties:} rows of golden test cases from the properties the compiler
 * infers.
 *
 * <pre>
 * bazelisk run //:generate-property-assertions -- tests/test-cases/functions/pow.yaml
 * bazelisk run //:generate-property-assertions -- --check tests/test-cases
 * </pre>
 *
 * <p>Each case is compiled on every jq version it has an expectation for, and the versions that
 * agree are joined into one {@code v:} range. A version the query does not compile on contributes
 * no row, which is how a case calling a builtin that arrived in jq 1.6 ends up with a range
 * starting there.
 */
public final class PropertyAssertionGenerator {
	private static final Jackson2JsonProvider PROVIDER = Jackson2JsonProvider.getInstance();

	/**
	 * The properties a case asserts, as the three fields a row carries.
	 */
	private record Properties(Cardinality cardinality, boolean dependsOnInput, boolean dependsOnExternalState) {
	}

	private static Map<Version, Properties> infer(TestCase tc, @Nullable Path moduleRoot) {
		Map<Version, Properties> properties = new LinkedHashMap<>();
		for (Version version : Versions.versions()) {
			if (!tc.expectations.hasDefault(version))
				continue;
			try {
				JsonQuery<JsonNode> query = environment(version, moduleRoot).compile(tc.q);
				ExpressionProperties props = query.getProperties();
				properties.put(version, new Properties(props.cardinality(), props.dependsOnInput(), props.dependsOnExternalState()));
			} catch (Exception doesNotCompile) {
				// A version the query does not compile on has no properties to assert.
			}
		}
		return properties;
	}

	/**
	 * The {@code properties:} rows one case asserts, empty when the query compiles on no version it
	 * expects.
	 *
	 * @param tc the case to infer for
	 * @param moduleRoot the module search root the case needs, or null when it needs none
	 * @return one row per version range, in version order
	 */
	public static List<TestCase.PropertyAssertion> assertionsFor(TestCase tc, @Nullable Path moduleRoot) {
		return rows(infer(tc, moduleRoot));
	}

	private static List<TestCase.PropertyAssertion> rows(Map<Version, Properties> properties) {
		List<TestCase.PropertyAssertion> rows = new ArrayList<>();
		for (VersionedRows.Row<Properties> row : VersionedRows.merge(Versions.versions(), properties)) {
			rows.add(new TestCase.PropertyAssertion(VersionRange.valueOf(row.range()), row.value().cardinality(),
					row.value().dependsOnInput(), row.value().dependsOnExternalState()));
		}
		return rows;
	}

	private static Environment<JsonNode> environment(Version version, @Nullable Path moduleRoot) throws Exception {
		EnvironmentBuilder<JsonNode> builder = EnvironmentBuilder.withDefaultLoaders(PROVIDER, version);
		if (moduleRoot != null) {
			builder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(PROVIDER, moduleRoot))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		return builder
				// Regex is an extension module; the suite includes it because jq's test cases call
				// test, match, sub and the rest by their bare names.
				.includeModule(new JoniRegexModule())
				.defineVariable("ENV", () -> PROVIDER.createObject(Collections.singletonMap("PAGER", PROVIDER.createString("less"))))
				.build();
	}

	/**
	 * Writes each case's rows, or reports the ones that differ from what is committed.
	 */
	private static int generate(Path file, boolean check) throws Exception {
		TestCaseFormatter.Document document = TestCaseFormatter.read(file);
		@Var int changed = 0;
		for (TestCaseFormatter.Entry entry : document.entries()) {
			TestCase tc = entry.testCase();
			Path moduleRoot = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
			try {
				List<TestCase.PropertyAssertion> rows = assertionsFor(tc, moduleRoot);
				if (check) {
					if (!equal(tc.properties, rows)) {
						changed++;
						System.out.printf("%s: '%s' properties differ%n  committed: %s%n  inferred:  %s%n",
								file, tc.q, render(tc.properties), render(rows));
					}
					continue;
				}
				tc.properties = rows;
				changed++;
			} finally {
				if (moduleRoot != null)
					ModuleFixtures.cleanup(moduleRoot);
			}
		}
		if (!check) {
			TestCaseFormatter.write(file, document);
			System.out.printf("%s: wrote properties for %d of %d cases%n", file, changed, document.entries().size());
		}
		return changed;
	}

	private static boolean equal(List<TestCase.PropertyAssertion> committed, List<TestCase.PropertyAssertion> inferred) {
		if (committed.size() != inferred.size())
			return false;
		for (int i = 0; i < committed.size(); i++) {
			TestCase.PropertyAssertion a = committed.get(i);
			TestCase.PropertyAssertion b = inferred.get(i);
			if (!a.version.equals(b.version) || a.cardinality != b.cardinality
					|| a.dependsOnInput != b.dependsOnInput || a.dependsOnExternalState != b.dependsOnExternalState)
				return false;
		}
		return true;
	}

	/**
	 * Rows as the report shows them; their own toString leaves the range out.
	 */
	private static String render(List<TestCase.PropertyAssertion> rows) {
		List<String> rendered = new ArrayList<>();
		for (TestCase.PropertyAssertion row : rows) {
			rendered.add(String.format("{v: '%s', cardinality: %s, depends_on_input: %s, depends_on_external_state: %s}",
					TestCaseFormatter.range(row.version), row.cardinality, row.dependsOnInput, row.dependsOnExternalState));
		}
		return String.join(", ", rendered);
	}

	public static void main(String[] args) throws Exception {
		TestCaseFiles.Invocation invocation = TestCaseFiles.parse(args, "generate-property-assertions");
		@Var int differing = 0;
		for (Path file : invocation.files()) {
			if (generate(file, invocation.check()) > 0 && invocation.check())
				differing++;
		}
		if (invocation.check()) {
			System.out.printf("%d of %d files differ%n", differing, invocation.files().size());
			if (differing > 0)
				System.exit(1);
		}
	}

	private PropertyAssertionGenerator() {
	}
}
