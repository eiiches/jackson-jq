package net.thisptr.jackson.jq.v2.test.expectations;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.comparator.FloatTolerance;
import net.thisptr.jackson.jq.v2.test.evaluator.EvaluationLimits;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.JqExecutables;
import net.thisptr.jackson.jq.v2.test.evaluator.JqRunner;
import net.thisptr.jackson.jq.v2.test.testcase.ExpectationComparison;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFiles;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFormatter;
import net.thisptr.jackson.jq.v2.test.testcase.VersionedRows;

/**
 * Regenerates real jq results and the jackson-jq results that differ from them.
 */
public final class ExpectationGenerator {
	private static final Duration TIMEOUT = Duration.ofSeconds(20);
	private static final Jackson2JsonProvider PROVIDER = Jackson2JsonProvider.getInstance();
	private static final Map<String, String> TIMEZONES = Map.of(
			"tokyo.yaml", "Asia/Tokyo",
			"kiritimati.yaml", "Pacific/Kiritimati",
			"howland.yaml", "Etc/GMT+12",
			"chatham.yaml", "Pacific/Chatham");

	private record Outcome(List<JsonNode> values, Evaluator.@Nullable ErrorPhase errorPhase, @Nullable String stderr,
						   boolean timeout,
						   boolean skip) {
		private static Outcome skipped() {
			return new Outcome(List.of(), null, "", false, true);
		}

		private static Outcome timedOut() {
			return new Outcome(List.of(), null, "", true, false);
		}

		/**
		 * Whether two observations belong in one row, by the same reading the validator applies to
		 * the rows afterwards: the output is compared by value, so versions that differ only in how
		 * jq writes a number share a row.
		 */
		private static boolean same(Outcome a, Outcome b, @Nullable FloatTolerance tolerance) {
			return a.errorPhase() == b.errorPhase() && Objects.equals(a.stderr(), b.stderr())
					&& a.timeout() == b.timeout() && a.skip() == b.skip()
					&& ExpectationComparison.sameValues(a.values(), b.values(), tolerance);
		}
	}

	private record JjqOutcome(@Nullable List<JsonNode> values, Evaluator.@Nullable ErrorPhase errorPhase,
							  @Nullable String message, boolean limitExceeded, TestCase.IncompatibilityType type) {
		/**
		 * Whether two observations belong in one row. The recorded output is compared by value, the
		 * way every assertion reads it, so versions that differ only in how jackson-jq writes a
		 * number -- an integer on one and the same value as a float on the next -- share a row.
		 */
		private static boolean same(JjqOutcome a, JjqOutcome b, @Nullable FloatTolerance tolerance) {
			return a.errorPhase() == b.errorPhase() && Objects.equals(a.message(), b.message())
					&& a.limitExceeded() == b.limitExceeded() && a.type() == b.type()
					&& ExpectationComparison.sameValues(a.values(), b.values(), tolerance);
		}
	}

	private static List<TestCase.DefaultExpectation> generateJq(TestCase tc, @Nullable Path moduleRoot, @Nullable String timezone) throws Exception {
		Map<Version, Outcome> outcomes = new LinkedHashMap<>();
		for (Version version : Versions.versions()) {
			TestCase.AbstractExpectation previous = tc.expectations.resolve(version, true, "Linux", "amd64");
			if (previous.unstable()) {
				outcomes.put(version, Outcome.skipped());
				continue;
			}
			String executable = JqExecutables.executableFor(version);
			try {
				Evaluator.Result result = new JqRunner(executable, moduleRoot, timezone).evaluate(tc.q, tc.input, TIMEOUT);
				outcomes.put(version, new Outcome(result.values(), result.errorPhase(), result.stderr(), false, false));
			} catch (TimeoutException timedOut) {
				outcomes.put(version, Outcome.timedOut());
			}
		}
		return VersionedRows.merge(Versions.versions(), outcomes,
				(a, b) -> Outcome.same(a, b, tc.floatTolerance)).stream().map(row -> {
			TestCase.DefaultExpectation expectation = new TestCase.DefaultExpectation(VersionRange.valueOf(row.range()));
			Outcome outcome = row.value();
			if (outcome.skip()) {
				expectation.skip = true;
			} else if (outcome.timeout()) {
				expectation.timeout = true;
			} else if (outcome.errorPhase() == Evaluator.ErrorPhase.COMPILE) {
				expectation.compileError = outcome.stderr();
			} else {
				expectation.output = outcome.values();
				if (outcome.errorPhase() == Evaluator.ErrorPhase.RUNTIME)
					expectation.runtimeError = outcome.stderr();
			}
			return expectation;
		}).toList();
	}

	private static TestCase.JacksonJqExpectation evaluateJjq(TestCase tc, Version version, @Nullable Path moduleRoot) throws Exception {
		TestCase.JacksonJqExpectation actual = new TestCase.JacksonJqExpectation(VersionRange.of(null, false, null, false));
		EnvironmentBuilder<JsonNode> builder = EnvironmentBuilder.withDefaultLoaders(PROVIDER, version);
		if (moduleRoot != null) {
			builder.clearModuleLoaders()
					.addModuleLoader(new FileSystemModuleLoader<>(PROVIDER, moduleRoot))
					.addModuleLoader(ClassPathModuleLoader.getInstance());
		}
		Environment<JsonNode> environment = builder
				.includeModule(new JoniRegexModule())
				.defineVariable("ENV", () -> PROVIDER.createObject(Collections.singletonMap("PAGER", PROVIDER.createString("less"))))
				.build();
		List<JsonNode> values = new ArrayList<>();
		@Var boolean compiled = false;
		try {
			JsonQuery<JsonNode> query = environment.compile(tc.q);
			compiled = true;
			query.withRuntimeOptions(EvaluationLimits.OPTIONS)
					.apply(JsonCodec.parse(PROVIDER, tc.input.toString(), JsonNodeUtils.parseOptions(version)), values::add);
			actual.output = values;
		} catch (RuntimeLimitExceededException limitExceeded) {
			actual.output = values;
			actual.limitExceeded = true;
		} catch (JsonQueryException queryError) {
			@Var String message = queryError.getMessage();
			if (message == null || message.isBlank())
				message = queryError.getClass().getSimpleName();
			if (compiled) {
				actual.output = values;
				actual.runtimeError = message;
			} else {
				actual.compileError = message;
			}
		}
		return actual;
	}

	private static List<TestCase.JacksonJqExpectation> generateJjq(TestCase tc, @Nullable Path moduleRoot,
			List<TestCase.JacksonJqExpectation> previousRows) throws Exception {
		Map<Version, JjqOutcome> outcomes = new LinkedHashMap<>();
		for (Version version : Versions.versions()) {
			TestCase.AbstractExpectation reference = tc.expectations.resolve(version, true, "Linux", "amd64");
			if (reference.unstable())
				continue;
			TestCase.JacksonJqExpectation actual;
			try {
				actual = evaluateJjq(tc, version, moduleRoot);
			} catch (Exception failure) {
				throw new IllegalStateException("jackson-jq evaluation failed for jq " + version + " query " + tc.q, failure);
			}
			if (ExpectationComparison.equivalent(reference, actual, tc.floatTolerance))
				continue;
			@Var TestCase.IncompatibilityType type = TestCase.IncompatibilityType.UNCLASSIFIED;
			for (TestCase.JacksonJqExpectation previous : previousRows) {
				if (previous.contains(version)) {
					if (previous.incompatType != null)
						type = previous.incompatType;
					break;
				}
			}
			outcomes.put(version, new JjqOutcome(actual.output,
					actual.compileError != null ? Evaluator.ErrorPhase.COMPILE
							: actual.runtimeError != null ? Evaluator.ErrorPhase.RUNTIME : null,
					actual.compileError != null ? actual.compileError : actual.runtimeError,
					actual.limitExceeded, type));
		}
		return VersionedRows.merge(Versions.versions(), outcomes,
				(a, b) -> JjqOutcome.same(a, b, tc.floatTolerance)).stream().map(row -> {
			JjqOutcome outcome = row.value();
			TestCase.JacksonJqExpectation expectation = new TestCase.JacksonJqExpectation(VersionRange.valueOf(row.range()));
			expectation.incompatType = outcome.type();
			expectation.output = outcome.values();
			expectation.limitExceeded = outcome.limitExceeded();
			if (outcome.errorPhase() == Evaluator.ErrorPhase.COMPILE)
				expectation.compileError = outcome.message();
			else if (outcome.errorPhase() == Evaluator.ErrorPhase.RUNTIME)
				expectation.runtimeError = outcome.message();
			return expectation;
		}).toList();
	}

	private static void generate(Path file) throws Exception {
		TestCaseFormatter.Document document = TestCaseFormatter.readUnchecked(file);
		Path parent = file.getParent();
		String timezone = parent != null && parent.getFileName().toString().equals("timezones")
				? TIMEZONES.get(file.getFileName().toString()) : null;
		TimeZone previousTimezone = TimeZone.getDefault();
		if (timezone != null)
			TimeZone.setDefault(TimeZone.getTimeZone(timezone));
		try {
			for (TestCaseFormatter.Entry entry : document.entries()) {
				TestCase tc = entry.testCase();
				Path moduleRoot = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
				try {
					List<TestCase.JacksonJqExpectation> previousRows = tc.expectations.jjq;
					tc.expectations.defaultRows = generateJq(tc, moduleRoot, timezone);
					tc.expectations.jjq = generateJjq(tc, moduleRoot, previousRows);
					tc.expectations.validate();
				} finally {
					if (moduleRoot != null)
						ModuleFixtures.cleanup(moduleRoot);
				}
			}
			TestCaseFormatter.write(file, document);
		} finally {
			TimeZone.setDefault(previousTimezone);
		}
		System.out.printf("%s: generated expectations for %d cases%n", file, document.entries().size());
	}

	public static void main(String[] args) throws Exception {
		TestCaseFiles.Invocation invocation = TestCaseFiles.parse(args, "generate-expectations");
		if (invocation.check())
			throw new IllegalArgumentException("generate-expectations does not support --check");
		for (Path file : invocation.files())
			generate(file);
	}

	private ExpectationGenerator() {
	}
}
