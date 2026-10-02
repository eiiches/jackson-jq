package net.thisptr.jackson.jq.v2.test.typecheck;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
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
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFiles;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFormatter;
import net.thisptr.jackson.jq.v2.test.testcase.VersionedRows;

/**
 * Rewrites the {@code types:} rows of golden test cases from the types the compiler infers.
 *
 * <pre>
 * bazelisk run //:generate-type-assertions -- tests/test-cases/functions/pow.yaml
 * bazelisk run //:generate-type-assertions -- --check tests/test-cases
 * </pre>
 *
 * <p>Each case is compiled on every jq version it has an expectation for, once for an unknown input
 * and once for its own, and the versions that agree are joined into one {@code v:} range. A version
 * the query does not compile on contributes no row, which is how a case calling a builtin that
 * arrived in jq 1.6 ends up with a range starting there.
 */
public final class TypeAssertionGenerator {
	private static final Jackson2JsonProvider PROVIDER = Jackson2JsonProvider.getInstance();

	/**
	 * The output types inferred for one case, keyed by the input type they were inferred for.
	 */
	private static Map<String, Map<Version, String>> infer(TestCase tc, @Nullable Path moduleRoot) {
		Type inputType = tc.input.isNull() ? NullType.getInstance() : ConstantTypes.shapeOf(PROVIDER, tc.input);
		List<Type> inputTypes = inputType instanceof AnyType
				? List.of(AnyType.getInstance())
				: List.of(AnyType.getInstance(), inputType);
		Map<String, Map<Version, String>> outputs = new LinkedHashMap<>();
		for (Version version : Versions.versions()) {
			if (!tc.expectations.hasDefault(version))
				continue;
			try {
				Environment<JsonNode> environment = environment(version, moduleRoot);
				for (Type input : inputTypes) {
					CompileOptions options = CompileOptions.newBuilder()
							.setTypeCheckMode(TypeCheckMode.WARN)
							.setInputType(input)
							.build();
					JsonQuery<JsonNode> query = environment.compile(tc.q, options);
					outputs.computeIfAbsent(input.toString(), key -> new LinkedHashMap<>())
							.put(version, query.getType().outputType().toString());
				}
			} catch (Exception doesNotCompile) {
				// A version the query does not compile on has no inferred type to assert.
			}
		}
		return outputs;
	}

	/**
	 * The {@code types:} rows one case asserts, empty when the query compiles on no version it
	 * expects.
	 *
	 * @param tc the case to infer for
	 * @param moduleRoot the module search root the case needs, or null when it needs none
	 * @return one row per input type and version range, in version order
	 */
	public static List<TestCase.TypeAssertion> assertionsFor(TestCase tc, @Nullable Path moduleRoot) {
		return rows(infer(tc, moduleRoot));
	}

	private static List<TestCase.TypeAssertion> rows(Map<String, Map<Version, String>> outputs) {
		List<TestCase.TypeAssertion> rows = new ArrayList<>();
		for (Map.Entry<String, Map<Version, String>> entry : outputs.entrySet()) {
			for (VersionedRows.Row<String> row : VersionedRows.merge(Versions.versions(), entry.getValue())) {
				TestCase.TypeAssertion assertion = new TestCase.TypeAssertion(VersionRange.valueOf(row.range()));
				assertion.input = entry.getKey();
				assertion.output = row.value();
				rows.add(assertion);
			}
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
				List<TestCase.TypeAssertion> rows = assertionsFor(tc, moduleRoot);
				if (check) {
					if (!equal(tc.types, rows)) {
						changed++;
						System.out.printf("%s: '%s' types differ%n  committed: %s%n  inferred:  %s%n",
								file, tc.q, render(tc.types), render(rows));
					}
					continue;
				}
				tc.types = rows;
				changed++;
			} finally {
				if (moduleRoot != null)
					ModuleFixtures.cleanup(moduleRoot);
			}
		}
		if (!check) {
			TestCaseFormatter.write(file, document);
			System.out.printf("%s: wrote types for %d of %d cases%n", file, changed, document.entries().size());
		}
		return changed;
	}

	private static boolean equal(List<TestCase.TypeAssertion> committed, List<TestCase.TypeAssertion> inferred) {
		if (committed.size() != inferred.size())
			return false;
		for (int i = 0; i < committed.size(); i++) {
			TestCase.TypeAssertion a = committed.get(i);
			TestCase.TypeAssertion b = inferred.get(i);
			if (!a.version.equals(b.version) || !a.input.equals(b.input) || !a.output.equals(b.output))
				return false;
		}
		return true;
	}

	/**
	 * Rows as the report shows them; their own toString leaves the range out.
	 */
	private static String render(List<TestCase.TypeAssertion> rows) {
		List<String> rendered = new ArrayList<>();
		for (TestCase.TypeAssertion row : rows)
			rendered.add(String.format("{v: '%s', input: '%s', output: '%s'}", TestCaseFormatter.range(row.version), row.input, row.output));
		return String.join(", ", rendered);
	}

	public static void main(String[] args) throws Exception {
		TestCaseFiles.Invocation invocation = TestCaseFiles.parse(args, "generate-type-assertions");
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

	private TypeAssertionGenerator() {
	}
}
