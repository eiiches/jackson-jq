package net.thisptr.jackson.jq.v2.test.download;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
import net.thisptr.jackson.jq.v2.test.comparator.FloatTolerance;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.JqExecutables;
import net.thisptr.jackson.jq.v2.test.evaluator.JqRunner;
import net.thisptr.jackson.jq.v2.test.properties.PropertyAssertionGenerator;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseFormatter;
import net.thisptr.jackson.jq.v2.test.testcase.VersionSpelling;
import net.thisptr.jackson.jq.v2.test.testcase.VersionedRows;
import net.thisptr.jackson.jq.v2.test.typecheck.TypeAssertionGenerator;

/**
 * Imports jq's own test corpora into the golden test cases under {@code tests/test-cases}.
 *
 * <pre>
 * bazelisk run //:download-test-cases -- --jq-version '[1.8.2, )' -o tests/test-cases
 * </pre>
 *
 * <p>Needs one real jq per release named {@code jq-1.5} through {@code jq-1.8.2}, and network
 * access to fetch the corpora. The Bazel target supplies these jq binaries through its runfiles.
 *
 * <p>Only files for releases in {@code --jq-version} are changed. A case belongs to the file of the
 * <em>earliest</em> release whose corpus declares it, so each of jq's releases contributes only what
 * it added and no case is stated twice. What a case asserts is
 * not what upstream wrote down but what jq does: every program is run on every configured jq, and the
 * releases that agree are joined into one {@code v:} range. Where a corpus does declare an output,
 * the two are compared and a disagreement is reported -- it means either a jq build that is not the
 * release it claims, or a test upstream never ran.
 *
 * <p>{@code types:} and {@code properties:} are inferred in the same pass, by the same code the two
 * assertion generators use. Everything a case carries that neither a corpus nor the compiler can
 * supply -- {@code jjq:} rows naming a divergence of this library, platform {@code overrides:},
 * {@code float_tolerance:}, {@code modules:}, prose -- is read off the committed corpus first and
 * carried over, following its case if the release it belongs to has changed.
 */
public final class DownloadTestCases {
	private static final Duration JQ_TIMEOUT = Duration.ofSeconds(20);
	private static final Option JQ_VERSION_OPTION = Option.builder()
			.longOpt("jq-version")
			.hasArg()
			.required()
			.desc("jq version range whose generated files to write")
			.get();
	private static final Option OUTPUT_DIRECTORY_OPTION = Option.builder("o")
			.longOpt("output-directory")
			.hasArg()
			.required()
			.desc("directory containing the test case YAML files")
			.get();

	record Arguments(List<Version> versions, Path directory) {
	}

	/**
	 * How long the compiler gets to infer one case's assertions. jq's corpus holds programs it does
	 * not finish on -- a literal like {@code 5E500000000} sends constant folding into an exact
	 * BigInteger of half a billion digits -- and a case the assertion generators would hang on cannot
	 * be committed, so one that runs out of time is reported and left out.
	 */
	private static final Duration INFERENCE_TIMEOUT = Duration.ofSeconds(20);

	private static final ExecutorService INFERENCE = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "infer-assertions");
		// A compile that overran its time cannot be interrupted, only abandoned.
		thread.setDaemon(true);
		return thread;
	});

	private static final String LICENSE_HEADER = """
			# This file is generated from jqlang/jq files. Different license terms apply:
			#
			# jq is copyright (C) 2012 Stephen Dolan
			#
			# Permission is hereby granted, free of charge, to any person obtaining
			# a copy of this software and associated documentation files (the
			# "Software"), to deal in the Software without restriction, including
			# without limitation the rights to use, copy, modify, merge, publish,
			# distribute, sublicense, and/or sell copies of the Software, and to
			# permit persons to whom the Software is furnished to do so, subject to
			# the following conditions:
			#
			# The above copyright notice and this permission notice shall be
			# included in all copies or substantial portions of the Software.
			#
			# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
			# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
			# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
			# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
			# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
			# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
			# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
			#
			# jq's documentation (everything found under the docs/ subdirectory in
			# the source tree) is licensed under the Creative Commons CC BY 3.0
			# license, which can be found at:
			#
			#          https://creativecommons.org/licenses/by/3.0/
			#
			""";

	/**
	 * A case's identity: what decides whether two corpora, or two releases, mean the same test.
	 */
	private record CaseKey(String program, String input) {
		static CaseKey of(String program, JsonNode input) {
			return new CaseKey(program, input.toString());
		}
	}

	// jq 1.5's string multiplication reads its original string after growing the result has freed it.
	// The same case can then print different strings or fail with invalid UTF-8, depending on heap
	// layout. There is no stable jq 1.5 result to record or verify for this case.
	private static final CaseKey JQ_15_UNDEFINED_MULTIPLICATION =
			new CaseKey(". * 100000 | [.[:10],.[-10:]]", "\"abc\"");

	/**
	 * A case as the committed corpus holds it, minus everything this tool regenerates.
	 *
	 * @param jjq rows naming where this library diverges from jq
	 * @param overrides rows naming where a platform diverges from the rest
	 * @param floatTolerance how many ulps the case allows
	 * @param modules the jq modules the case needs on the search path
	 * @param comment the case's own comment field
	 * @param justification why the case asserts what it does
	 * @param leadingComment the comment block written above the case
	 */
	private record Curated(
			List<TestCase.JacksonJqExpectation> jjq,
			List<TestCase.OverrideExpectation> overrides,
			@Nullable FloatTolerance floatTolerance,
			Map<String, String> modules,
			@Nullable String comment,
			@Nullable String justification,
			String leadingComment) {
	}

	/**
	 * What jq did with one case on one version. Equality is what joins neighbouring releases into one
	 * expectation row, so it has to be the whole observable result.
	 *
	 * @param values what jq printed, or null when it never finished
	 * @param error whether jq exited non-zero
	 * @param timeout whether jq had to be killed
	 * @param skip whether this jq release has undefined behavior for the case
	 */
	private record Outcome(@Nullable List<JsonNode> values, Evaluator.@Nullable ErrorPhase errorPhase,
						   @Nullable String stderr,
						   boolean timeout, boolean skip) {
		boolean error() {
			return errorPhase != null;
		}
	}

	/**
	 * A case to import: where it first appeared, and what each release declares for it.
	 *
	 * @param introduced the earliest release whose corpus declares the case
	 * @param first the case as that release writes it
	 * @param declaredBy the case as every release that has it writes it
	 */
	private record Imported(
			Version introduced,
			UpstreamCorpus.UpstreamCase first,
			Map<Version, UpstreamCorpus.UpstreamCase> declaredBy) {
	}

	public static void main(String[] args) throws Exception {
		Arguments arguments = arguments(args);
		Path directory = arguments.directory();
		requireEveryJqBinary();

		Map<CaseKey, Curated> curated = new LinkedHashMap<>();
		Set<CaseKey> handWritten = new LinkedHashSet<>();
		readCommittedCorpus(directory, curated, handWritten);
		System.out.printf("read %d committed cases to carry over and %d hand-written ones to leave alone%n",
				curated.size(), handWritten.size());

		List<String> report = Collections.synchronizedList(new ArrayList<>());
		@Var int written = 0;
		for (UpstreamCorpus.Kind kind : UpstreamCorpus.Kind.values()) {
			Map<Version, List<Imported>> byRelease = imported(kind, handWritten, report);
			for (Version version : arguments.versions()) {
				Path file = directory.resolve(kind.fileName(version));
				List<Imported> cases = byRelease.getOrDefault(version, List.of());
				if (cases.isEmpty()) {
					if (Files.deleteIfExists(file))
						System.out.printf("%s: deleted, jq %s adds no case to %s%n", file.getFileName(), version, kind);
					continue;
				}
				List<TestCaseFormatter.Entry> entries = entries(cases, curated, file.getFileName().toString(), report);
				Files.writeString(file, LICENSE_HEADER + TestCaseFormatter.render(new TestCaseFormatter.Document(entries, "")),
						StandardCharsets.UTF_8);
				written++;
				System.out.printf("%s: wrote %d cases%n", file.getFileName(), entries.size());
			}
		}

		System.out.printf("%nwrote %d files%n", written);
		if (!report.isEmpty()) {
			System.err.printf("%n%d cases need a look:%n", report.size());
			report.stream().sorted().forEach(line -> System.err.println("  " + line));
		}
	}

	static Arguments arguments(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption(JQ_VERSION_OPTION);
		options.addOption(OUTPUT_DIRECTORY_OPTION);
		CommandLine command = new DefaultParser().parse(options, args);
		if (!command.getArgList().isEmpty())
			throw new IllegalArgumentException("unexpected positional arguments: " + command.getArgList());
		String jqVersion = command.getOptionValue(JQ_VERSION_OPTION.getLongOpt());
		VersionRange range = VersionRange.valueOf(jqVersion);
		List<Version> versions = Versions.versions().stream().filter(range::contains).toList();
		if (versions.isEmpty())
			throw new IllegalArgumentException("jq version range selects no supported releases: " + jqVersion);
		String workingDirectory = System.getenv("BUILD_WORKING_DIRECTORY");
		Path base = workingDirectory != null && !workingDirectory.isBlank() ? Path.of(workingDirectory) : Path.of("");
		Path directory = base.resolve(command.getOptionValue(OUTPUT_DIRECTORY_OPTION.getLongOpt()));
		if (!Files.isDirectory(directory))
			throw new IllegalArgumentException("no such directory: " + directory);
		return new Arguments(versions, directory);
	}

	/**
	 * A case has to be run on every configured release to say anything about it, so a jq that is not
	 * there is a reason to stop rather than to write expectations with a hole in them.
	 */
	private static void requireEveryJqBinary() {
		List<Version> missing = new ArrayList<>(Versions.versions());
		for (JqExecutables.JqExecutable executable : JqExecutables.ALL) {
			if (JqRunner.hasJq(executable.executable()))
				missing.remove(executable.jqVersion());
		}
		if (!missing.isEmpty()) {
			throw new IllegalStateException("no working jq for " + missing
					+ "; check the jq binaries supplied to this command");
		}
	}

	/**
	 * Indexes the corpus as committed: what to carry over from the files this tool writes, and which
	 * cases a hand-written file already states, which this tool must not state again.
	 */
	private static void readCommittedCorpus(Path directory, Map<CaseKey, Curated> curated, Set<CaseKey> handWritten)
			throws IOException {
		Set<String> owned = new LinkedHashSet<>();
		for (UpstreamCorpus.Kind kind : UpstreamCorpus.Kind.values()) {
			for (Version version : Versions.versions())
				owned.add(kind.fileName(version));
		}
		List<Path> files;
		try (Stream<Path> walk = Files.walk(directory)) {
			files = walk.filter(path -> path.toString().endsWith(".yaml")).sorted().toList();
		}
		for (Path file : files) {
			boolean own = owned.contains(file.getFileName().toString()) && directory.equals(file.getParent());
			TestCaseFormatter.Document document = TestCaseFormatter.read(file);
			for (int i = 0; i < document.entries().size(); i++) {
				TestCaseFormatter.Entry entry = document.entries().get(i);
				TestCase tc = entry.testCase();
				CaseKey key = CaseKey.of(tc.q, tc.input);
				if (!own) {
					handWritten.add(key);
					continue;
				}
				// The comment above the first case of a file this tool writes is its own licence
				// header, not something written about that case.
				String leadingComment = i == 0 ? "" : entry.comment();
				curated.put(key, new Curated(tc.expectations.jjq, tc.expectations.overrides, tc.floatTolerance,
						tc.modules, tc.comment, tc.justification, leadingComment));
			}
		}
	}

	/**
	 * Fetches one corpus from every release and groups its cases by the release that introduced them.
	 */
	private static Map<Version, List<Imported>> imported(UpstreamCorpus.Kind kind, Set<CaseKey> handWritten, List<String> report)
			throws IOException, InterruptedException {
		Map<CaseKey, Imported> cases = new LinkedHashMap<>();
		Set<CaseKey> unreadable = new LinkedHashSet<>();
		for (Version version : Versions.versions()) {
			UpstreamCorpus.Corpus corpus = UpstreamCorpus.fetch(kind, version);
			for (UpstreamCorpus.Unreadable left : corpus.unreadable()) {
				// Every release since restates the case, and saying so once is enough.
				if (unreadable.add(new CaseKey(left.program(), left.input()))) {
					report.add(String.format("%s: '%s' left out, its input is not JSON: %s",
							left.origin(), left.program(), left.input()));
				}
			}
			for (UpstreamCorpus.UpstreamCase upstream : corpus.cases()) {
				CaseKey key = CaseKey.of(upstream.program(), upstream.input());
				if (handWritten.contains(key))
					continue;
				cases.computeIfAbsent(key, ignored -> new Imported(version, upstream, new LinkedHashMap<>()))
						.declaredBy().put(version, upstream);
			}
		}
		Map<Version, List<Imported>> byRelease = new LinkedHashMap<>();
		cases.values().forEach(value -> byRelease.computeIfAbsent(value.introduced(), ignored -> new ArrayList<>()).add(value));
		return byRelease;
	}

	/**
	 * Builds one file's cases, running jq and the compiler over each of them.
	 */
	private static List<TestCaseFormatter.Entry> entries(
			List<Imported> cases, Map<CaseKey, Curated> curated, String file, List<String> report) {
		TestCaseFormatter.@Nullable Entry[] built = new TestCaseFormatter.Entry[cases.size()];
		IntStream.range(0, cases.size()).parallel()
				.forEach(i -> built[i] = entry(cases.get(i), curated, file, report));
		List<TestCaseFormatter.Entry> entries = new ArrayList<>();
		for (TestCaseFormatter.Entry entry : built) {
			if (entry != null)
				entries.add(entry);
		}
		return entries;
	}

	private static TestCaseFormatter.@Nullable Entry entry(
			Imported imported, Map<CaseKey, Curated> curated, String file, List<String> report) {
		UpstreamCorpus.UpstreamCase upstream = imported.first();
		Curated carried = curated.get(CaseKey.of(upstream.program(), upstream.input()));

		TestCase tc = new TestCase();
		tc.q = upstream.program();
		tc.input = upstream.input();
		if (carried != null) {
			tc.floatTolerance = carried.floatTolerance();
			tc.modules = carried.modules();
			tc.comment = carried.comment();
			tc.justification = carried.justification();
		}

		@Var Path moduleRoot = null;
		try {
			if (!tc.modules.isEmpty())
				moduleRoot = ModuleFixtures.materialize(tc.modules);
			Map<Version, Outcome> outcomes = run(tc, moduleRoot);
			if (neverRan(imported, outcomes)) {
				throw new IllegalStateException("jq rejects it outright on every release though the corpus"
						+ " declares output, so it needs something this tool does not give it -- a modules: block, most likely");
			}
			crossCheck(imported, outcomes, file, report);
			tc.expectations.defaultRows = defaultRows(outcomes);
			if (carried != null)
				carry(tc, carried, file, report);
			tc.expectations.validate();
			tc.expectations.validateCoverage(Versions.versions());
			Path root = moduleRoot;
			tc.types = infer(() -> TypeAssertionGenerator.assertionsFor(tc, root));
			tc.properties = infer(() -> PropertyAssertionGenerator.assertionsFor(tc, root));
			tc.validateTypes();
			tc.validateProperties();

			TestCaseFormatter.Entry entry =
					new TestCaseFormatter.Entry(tc, carried != null ? carried.leadingComment() : "", true);
			// A value jq prints that the golden format cannot spell -- a NaN input, say -- is better
			// left out than written as something else, so the case is rendered before it is kept.
			TestCaseFormatter.render(new TestCaseFormatter.Document(List.of(entry), ""));
			return entry;
		} catch (Exception unusable) {
			report.add(String.format("%s: '%s' from %s dropped: %s", file, tc.q, upstream.origin(), unusable));
			return null;
		} finally {
			if (moduleRoot != null) {
				try {
					ModuleFixtures.cleanup(moduleRoot);
				} catch (IOException leaked) {
					report.add(file + ": could not clean up " + moduleRoot + ": " + leaked);
				}
			}
		}
	}

	/**
	 * Infers one case's assertion rows, giving up on a program the compiler does not finish with.
	 */
	private static <T> List<T> infer(Callable<List<T>> inference) throws Exception {
		Future<List<T>> future = INFERENCE.submit(inference);
		try {
			return future.get(INFERENCE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException neverFinished) {
			future.cancel(true);
			throw new IllegalStateException("the compiler did not finish inferring in " + INFERENCE_TIMEOUT);
		} catch (ExecutionException failed) {
			throw new IllegalStateException("the compiler failed to infer", failed.getCause());
		}
	}

	/** Runs one case on every configured jq. */
	private static Map<Version, Outcome> run(TestCase tc, @Nullable Path moduleRoot) throws IOException, InterruptedException {
		Map<Version, Outcome> outcomes = new LinkedHashMap<>();
		for (JqExecutables.JqExecutable executable : JqExecutables.ALL) {
			if (executable.jqVersion().equals(Version.of(1, 5, 0))
					&& JQ_15_UNDEFINED_MULTIPLICATION.equals(CaseKey.of(tc.q, tc.input))) {
				outcomes.put(executable.jqVersion(), new Outcome(null, null, null, false, true));
				continue;
			}
			JqRunner runner = new JqRunner(executable.executable(), moduleRoot);
			try {
				Evaluator.Result result = runner.evaluate(tc.q, tc.input, JQ_TIMEOUT);
				outcomes.put(executable.jqVersion(), new Outcome(result.values(), result.errorPhase(), result.stderr(), false, false));
			} catch (TimeoutException neverFinished) {
				outcomes.put(executable.jqVersion(), new Outcome(null, null, null, true, false));
			}
		}
		return outcomes;
	}

	private static List<TestCase.DefaultExpectation> defaultRows(Map<Version, Outcome> outcomes) {
		List<TestCase.DefaultExpectation> rows = new ArrayList<>();
		for (VersionedRows.Row<Outcome> row : VersionedRows.merge(Versions.versions(), outcomes)) {
			TestCase.DefaultExpectation expectation = new TestCase.DefaultExpectation(VersionRange.valueOf(row.range()));
			if (row.value().skip()) {
				expectation.skip = true;
			} else if (row.value().timeout()) {
				expectation.timeout = true;
			} else {
				if (row.value().errorPhase() == Evaluator.ErrorPhase.COMPILE) {
					expectation.compileError = row.value().stderr();
				} else {
					expectation.output = row.value().values();
					if (row.value().errorPhase() == Evaluator.ErrorPhase.RUNTIME)
						expectation.runtimeError = row.value().stderr();
				}
			}
			rows.add(expectation);
		}
		return rows;
	}

	/**
	 * Puts the carried rows back, dropping the ones the regenerated expectations have outgrown: a
	 * divergence stated over a range the default rows no longer span, or one they now state
	 * themselves.
	 */
	private static void carry(TestCase tc, Curated carried, String file, List<String> report) {
		if (!carried.jjq().isEmpty()) {
			tc.expectations.jjq = carried.jjq();
			String stale = stale(tc);
			if (stale != null) {
				tc.expectations.jjq = List.of();
				report.add(String.format("%s: '%s' dropped its jjq rows: %s", file, tc.q, stale));
			}
		}
		if (!carried.overrides().isEmpty()) {
			tc.expectations.overrides = carried.overrides();
			String stale = stale(tc);
			if (stale != null) {
				tc.expectations.overrides = List.of();
				report.add(String.format("%s: '%s' dropped its overrides rows: %s", file, tc.q, stale));
			}
		}
	}

	/** Why the expectations as they stand do not hold together, or null when they do. */
	private static @Nullable String stale(TestCase tc) {
		try {
			tc.expectations.validate();
			return null;
		} catch (IllegalArgumentException invalid) {
			return invalid.getMessage();
		}
	}

	/**
	 * Whether jq never ran the case: it failed on every release without printing anything, while the
	 * corpus that published it declares output. A case like that would assert that jq rejects the
	 * program, which is not true -- it is this tool that did not give the program what it needs.
	 */
	private static boolean neverRan(Imported imported, Map<Version, Outcome> outcomes) {
		for (Outcome outcome : outcomes.values()) {
			if (outcome.skip())
				continue;
			if (outcome.timeout() || !outcome.error() || !Objects.requireNonNull(outcome.values()).isEmpty())
				return false;
		}
		for (UpstreamCorpus.UpstreamCase upstream : imported.declaredBy().values()) {
			List<JsonNode> declared = upstream.declaredOutput();
			if (declared != null && !declared.isEmpty())
				return true;
		}
		return false;
	}

	/**
	 * Whether a corpus and jq printed the same values. A corpus writes a number the way whoever wrote
	 * the test did and jq prints it the way jq does, so {@code 19.0} and {@code 19} are one output
	 * written down twice rather than a disagreement to report.
	 */
	private static boolean same(List<JsonNode> declared, List<JsonNode> printed) {
		if (declared.size() != printed.size())
			return false;
		for (int i = 0; i < declared.size(); i++) {
			if (!same(declared.get(i), printed.get(i)))
				return false;
		}
		return true;
	}

	private static boolean same(JsonNode declared, JsonNode printed) {
		if (declared.isNumber() && printed.isNumber()) {
			if (!finite(declared) || !finite(printed))
				return Double.compare(declared.doubleValue(), printed.doubleValue()) == 0;
			return declared.decimalValue().compareTo(printed.decimalValue()) == 0;
		}
		if (declared.isArray() && printed.isArray())
			return same(elements(declared), elements(printed));
		if (declared.isObject() && printed.isObject()) {
			if (declared.size() != printed.size())
				return false;
			for (Map.Entry<String, JsonNode> field : declared.properties()) {
				JsonNode value = printed.get(field.getKey());
				if (value == null || !same(field.getValue(), value))
					return false;
			}
			return true;
		}
		return declared.equals(printed);
	}

	private static boolean finite(JsonNode number) {
		return (!number.isDouble() && !number.isFloat()) || Double.isFinite(number.doubleValue());
	}

	private static List<JsonNode> elements(JsonNode array) {
		List<JsonNode> elements = new ArrayList<>();
		array.forEach(elements::add);
		return elements;
	}

	/** Reports where a corpus and the jq it was published with disagree about one case. */
	private static void crossCheck(Imported imported, Map<Version, Outcome> outcomes, String file, List<String> report) {
		imported.declaredBy().forEach((version, upstream) -> {
			Outcome outcome = outcomes.get(version);
			if (outcome == null || outcome.timeout() || outcome.skip())
				return;
			if (upstream.declaresFailure()) {
				if (!outcome.error()) {
					report.add(String.format("%s: '%s' is %%%%FAIL in %s but jq %s accepts it",
							file, upstream.program(), upstream.origin(), VersionSpelling.of(version)));
				}
				return;
			}
			List<JsonNode> declared = upstream.declaredOutput();
			if (declared == null)
				return;
			if (outcome.error()) {
				report.add(String.format("%s: '%s' declares output in %s but jq %s fails",
						file, upstream.program(), upstream.origin(), VersionSpelling.of(version)));
			} else if (!same(declared, Objects.requireNonNull(outcome.values()))) {
				report.add(String.format("%s: '%s' from %s: jq %s prints %s, not %s",
						file, upstream.program(), upstream.origin(), VersionSpelling.of(version),
						outcome.values(), declared));
			}
		});
	}

	private DownloadTestCases() {
	}
}
