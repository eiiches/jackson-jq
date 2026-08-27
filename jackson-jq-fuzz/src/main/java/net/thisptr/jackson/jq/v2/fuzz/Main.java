package net.thisptr.jackson.jq.v2.fuzz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.errorprone.annotations.Var;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.DoubleLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.LongLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.JqExecutables;
import net.thisptr.jackson.jq.v2.test.evaluator.JqRunner;

/**
 * Randomized differential-testing fuzzer: generates random jq ASTs and evaluates them with both a real
 * {@code jq} subprocess and jackson-jq's own engine, reporting any mismatches.
 */
public class Main {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Set<String> ALWAYS_EXCLUDED_FUNCTIONS = new HashSet<>(Arrays.asList(
			"now/0", // wall-clock time; always differs between the two evaluations, not a real mismatch
			"builtins/0" // the set of built-in functions inherently differs between jq and jackson-jq
	));

	private static final Map<Version, Set<String>> EXCLUDED_FUNCTIONS = new HashMap<>();

	static {
		EXCLUDED_FUNCTIONS.computeIfAbsent(Versions.JQ_1_5, k -> {
			return new HashSet<>(Arrays.asList(
					"log2/0" // log2 has slightly different precisions
			));
		});
		EXCLUDED_FUNCTIONS.computeIfAbsent(Versions.JQ_1_6, k -> {
			return new HashSet<>(Arrays.asList(
					"log2/0" // log2 has slightly different precisions
			));
		});
	}

	private static final Map<Version, List<Generator>> GENERATORS_CACHE = new HashMap<>();

	private static synchronized List<Generator> getGenerators(Version version) {
		return GENERATORS_CACHE.computeIfAbsent(version, v -> {
			List<Generator> generators = new ArrayList<>();
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.PLUS, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.MINUS, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.MODULO, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.AND, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.OR, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.DEFAULT, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(3, (exprs) -> new TupleAstNode(exprs)));
			generators.add(new RandomGenerator(1, (exprs) -> new ArrayConstructionAstNode(exprs.get(0))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.TIMES, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.DIVIDE, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(0, (exprs) -> new ThisObjectAstNode()));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), true)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), false)));
			generators.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), exprs.get(2), true)));
			generators.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), exprs.get(2), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(exprs.get(0), true)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(exprs.get(0), false)));

			Set<String> exclusions = new HashSet<>(ALWAYS_EXCLUDED_FUNCTIONS);
			exclusions.addAll(EXCLUDED_FUNCTIONS.getOrDefault(v, Collections.emptySet()));
			ClassPathFunctionLoader.getInstance().getFunctions(v).forEach((nameAndArity, factory) -> {
				String signature = nameAndArity.toString();
				if (exclusions.contains(signature))
					return;
				if (signature.contains("/")) {
					int numArgs = Integer.parseInt(signature.split("/", 2)[1]);
					String name = signature.split("/", 2)[0];
					if (exclusions.contains(name))
						return;
					generators.add(new RandomGenerator(numArgs, (exprs) -> new FunctionCallAstNode(null, name, exprs, v)));
				} else {
					generators.add(new RandomGenerator(0, 10, (exprs) -> new FunctionCallAstNode(null, signature, exprs, v)));
				}
			});
			return generators;
		});
	}

	private static List<AstNode> createInitialExpressions(Version version) {
		List<AstNode> expressions = new ArrayList<>();
		expressions.add(new BooleanLiteralAstNode(true));
		expressions.add(new BooleanLiteralAstNode(false));
		expressions.add(new LongLiteralAstNode(-1));
		expressions.add(new LongLiteralAstNode(0));
		expressions.add(new LongLiteralAstNode(1));
		expressions.add(new DoubleLiteralAstNode(-1.5));
		expressions.add(new DoubleLiteralAstNode(-1.0));
		expressions.add(new DoubleLiteralAstNode(-1.0));
		expressions.add(new DoubleLiteralAstNode(-0.5));
		expressions.add(new DoubleLiteralAstNode(0.0));
		expressions.add(new DoubleLiteralAstNode(0.5));
		expressions.add(new DoubleLiteralAstNode(1.0));
		expressions.add(new DoubleLiteralAstNode(1.5));
		expressions.add(new NullLiteralAstNode());
		expressions.add(new StringLiteralAstNode("foo"));
		expressions.add(new StringLiteralAstNode("bar"));
		expressions.add(new StringLiteralAstNode("baz"));
		expressions.add(new FunctionCallAstNode(null, "empty", Collections.emptyList(), version));
		expressions.add(new StringLiteralAstNode("\r"));
		expressions.add(new StringLiteralAstNode("\n"));
		expressions.add(new StringLiteralAstNode("\t"));
		expressions.add(new StringLiteralAstNode("\0"));
		return expressions;
	}

	public enum Category {
		UNEXPECTED_EXCEPTION,
		STATUS_MISMATCH,
		COUNT_MISMATCH,
		VALUE_MISMATCH,
		TIMEOUT,
		EVALUATION_ERROR
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class DiagnosticRecord {
		@JsonProperty("iteration")
		public int iteration;

		@JsonProperty("seed")
		public long seed;

		@JsonProperty("version")
		public String version = "";

		@JsonProperty("category")
		public Category category = Category.UNEXPECTED_EXCEPTION;

		@JsonProperty("message")
		public String message = "";

		@JsonProperty("expression")
		public String expression = "";

		@JsonProperty("in")
		public JsonNode in = NullNode.getInstance();

		@JsonProperty("expected")
		public @Nullable EvaluationSummary expected;

		@JsonProperty("actual")
		public @Nullable EvaluationSummary actual;

		@JsonProperty("testCase")
		public @Nullable TestCase testCase;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class EvaluationSummary {
		@JsonProperty("values")
		public List<JsonNode> values = Collections.emptyList();

		@JsonProperty("error")
		public @Nullable String error;

		@JsonProperty("errorType")
		public @Nullable String errorType;

		@JsonProperty("stackTrace")
		public @Nullable List<String> stackTrace;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SummaryRecord {
		@JsonProperty("summary")
		public boolean summary = true;

		@JsonProperty("totalIterations")
		public int totalIterations;

		@JsonProperty("passed")
		public int passed;

		@JsonProperty("errors")
		public int errors;

		@JsonProperty("categories")
		public Map<Category, Integer> categories = Collections.emptyMap();

		@JsonProperty("seed")
		public long seed;

		@JsonProperty("version")
		public String version = "";
	}

	private static @Nullable EvaluationSummary toSummary(Evaluator.@Nullable Result result) {
		if (result == null)
			return null;
		EvaluationSummary s = new EvaluationSummary();
		s.values = result.values != null ? result.values : Collections.emptyList();
		if (result.error != null) {
			s.error = result.error.getMessage();
			s.errorType = result.error.getClass().getName();
			if (!(result.error instanceof JsonQueryException)) {
				List<String> trace = new ArrayList<>();
				for (StackTraceElement elem : result.error.getStackTrace()) {
					trace.add(elem.toString());
					if (trace.size() >= 15)
						break;
				}
				s.stackTrace = trace;
			}
		}
		return s;
	}

	private static JsonProvider<?> resolveProvider(String name) {
		switch (name) {
			case "jackson2":
				return Jackson2JsonProviderImpl.getInstance();
			case "jackson3":
				return Jackson3JsonProviderImpl.getInstance();
			case "gson":
				return GsonJsonProviderImpl.getInstance();
			default:
				throw new IllegalArgumentException("unknown --provider: " + name + " (expected one of: jackson2, jackson3, gson)");
		}
	}

	private static <N> Evaluator createJacksonJqRunner(JsonProvider<N> jsonProvider, Version jqVersion) {
		return new JacksonJqRunner<>(jsonProvider, jqVersion);
	}

	private static final Option OPT_ITERATIONS = Option.builder()
			.longOpt("iterations")
			.desc("number of random expressions to generate and evaluate (default: 10000)")
			.numberOfArgs(1)
			.get();

	private static final Option OPT_JQ_VERSION = Option.builder()
			.longOpt("jq")
			.desc("jq version to test against (default: 1.5)")
			.numberOfArgs(1)
			.get();

	private static final Option OPT_JSON_PROVIDER = Option.builder()
			.longOpt("json-provider")
			.desc("JSON provider backing jackson-jq's own evaluation: jackson2, jackson3, or gson (default: jackson2)")
			.numberOfArgs(1)
			.get();

	private static final Option OPT_SEED = Option.builder()
			.longOpt("seed")
			.desc("random seed (default: random)")
			.numberOfArgs(1)
			.get();

	private static final Option OPT_TIMEOUT_MS = Option.builder()
			.longOpt("timeout-ms")
			.desc("per-evaluation timeout in milliseconds (default: 1000)")
			.numberOfArgs(1)
			.get();

	private static final Option OPT_FAIL_ON_ERROR = Option.builder()
			.longOpt("fail-on-error")
			.desc("exit with a non-zero status if any mismatch is found")
			.get();

	private static final Option OPT_IGNORE_FLOAT_ERRORS = Option.builder()
			.longOpt("ignore-float-errors")
			.desc("treat small floating-point precision differences between jq and jackson-jq as matches, not mismatches")
			.get();

	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.get();

	public static void main(String[] args) throws Throwable {
		Options options = new Options();
		options.addOption(OPT_ITERATIONS);
		options.addOption(OPT_JQ_VERSION);
		options.addOption(OPT_JSON_PROVIDER);
		options.addOption(OPT_SEED);
		options.addOption(OPT_TIMEOUT_MS);
		options.addOption(OPT_FAIL_ON_ERROR);
		options.addOption(OPT_IGNORE_FLOAT_ERRORS);
		options.addOption(OPT_HELP);

		CommandLine command;
		try {
			CommandLineParser parser = new DefaultParser();
			command = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("invalid arguments: " + Arrays.toString(args));
			System.exit(1);
			throw e;
		}

		if (command.hasOption(OPT_HELP.getOpt())) {
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("jackson-jq-fuzz [OPTIONS...]", null, options, null, false);
			System.exit(0);
		}

		int iterations = command.hasOption(OPT_ITERATIONS.getLongOpt())
				? Integer.parseInt(command.getOptionValue(OPT_ITERATIONS.getLongOpt()))
				: 10000;

		Version version = Version.valueOf(command.hasOption(OPT_JQ_VERSION.getLongOpt())
				? command.getOptionValue(OPT_JQ_VERSION.getLongOpt())
				: "1.5");
		if (!Versions.versions().contains(version)) {
			System.err.println("unsupported --jq version: " + version);
			System.exit(1);
		}

		String providerName = command.hasOption(OPT_JSON_PROVIDER.getLongOpt())
				? command.getOptionValue(OPT_JSON_PROVIDER.getLongOpt())
				: "jackson2";
		JsonProvider<?> jsonProvider;
		try {
			jsonProvider = resolveProvider(providerName);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			throw e;
		}

		long seed = command.hasOption(OPT_SEED.getLongOpt())
				? Long.parseLong(command.getOptionValue(OPT_SEED.getLongOpt()))
				: new Random().nextLong();

		long timeoutMs = command.hasOption(OPT_TIMEOUT_MS.getLongOpt())
				? Long.parseLong(command.getOptionValue(OPT_TIMEOUT_MS.getLongOpt()))
				: 1000L;
		Duration timeout = Duration.ofMillis(timeoutMs);

		boolean failOnError = command.hasOption(OPT_FAIL_ON_ERROR.getLongOpt());
		boolean ignoreFloatErrors = command.hasOption(OPT_IGNORE_FLOAT_ERRORS.getLongOpt());

		Random random = new Random(seed);
		List<Generator> generators = getGenerators(version);
		List<AstNode> expressions = createInitialExpressions(version);

		List<JsonNode> values = new ArrayList<>();
		Set<JsonNode> uniqueValues = new TreeSet<>(new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance()));
		values.add(NullNode.getInstance());
		uniqueValues.add(NullNode.getInstance());

		List<DiagnosticRecord> diagnostics = new ArrayList<>();
		@Var int passedCount = 0;

		JsonNodeComparator<JsonNode> nodeComparator = ignoreFloatErrors
				? new ToleranceJsonNodeComparator()
				: new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance());

		Evaluator actualEvaluator = createJacksonJqRunner(jsonProvider, version);

		for (int i = 0; i < iterations; ++i) {
			Generator generator = generators.get(random.nextInt(generators.size()));

			List<AstNode> args2 = new ArrayList<>();
			for (int j = 0; j < generator.args(); ++j)
				args2.add(expressions.get(random.nextInt(expressions.size())));

			AstNode expr = generator.generate(args2);
			JsonNode in = values.get(random.nextInt(values.size()));

			@Var Evaluator.Result expected = null;
			try {
				expected = new JqRunner(JqExecutables.executableFor(version)).evaluate(expr.toString(), in, timeout);
			} catch (Throwable e) {
				// jq itself could not evaluate (e.g. process error, timeout, crash)
				continue;
			}

			@Var Evaluator.Result actual = null;
			try {
				actual = actualEvaluator.evaluate(expr.toString(), in, timeout);
			} catch (Throwable e) {
				actual = new Evaluator.Result(Collections.emptyList(), e);
			}

			@Var Category category = null;
			@Var String message = "";

			if (actual.error != null && !(actual.error instanceof JsonQueryException)) {
				if (actual.error instanceof TimeoutException) {
					category = Category.TIMEOUT;
					message = "jackson-jq evaluation timed out";
				} else {
					category = Category.UNEXPECTED_EXCEPTION;
					message = "jackson-jq threw " + actual.error.getClass().getName() + ": " + actual.error.getMessage();
				}
			} else if ((expected.error != null) != (actual.error != null)) {
				category = Category.STATUS_MISMATCH;
				if (expected.error != null) {
					message = "jq failed with error [" + expected.error.getMessage() + "] but jackson-jq succeeded producing " + actual.values.size() + " value(s)";
				} else {
					message = "jq succeeded with " + expected.values.size() + " value(s) but jackson-jq failed with error [" + actual.error.getMessage() + "]";
				}
			} else if (expected.error == null && actual.error == null) {
				if (expected.values.size() != actual.values.size()) {
					category = Category.COUNT_MISMATCH;
					message = "Output count mismatch: expected " + expected.values.size() + " value(s), actual " + actual.values.size() + " value(s)";
				} else {
					for (int t = 0; t < expected.values.size(); ++t) {
						if (nodeComparator.compare(expected.values.get(t), actual.values.get(t)) != 0) {
							category = Category.VALUE_MISMATCH;
							message = "Output mismatch at index " + t + ": expected=" + expected.values.get(t) + ", actual=" + actual.values.get(t);
							break;
						}
					}
				}
			}

			if (category != null) {
				TestCase testCase = new TestCase();
				testCase.in = in;
				testCase.version = VersionRange.of(version, true, null, false);
				if (expected.error != null) {
					testCase.expression = new TryCatchAstNode(expr, new StringLiteralAstNode("__ERROR__"));
					testCase.out = new ArrayList<>(expected.values);
					testCase.out.add(TextNode.valueOf("__ERROR__"));
				} else {
					testCase.expression = expr;
					testCase.out = expected.values;
				}

				DiagnosticRecord record = new DiagnosticRecord();
				record.iteration = i;
				record.seed = seed;
				record.version = version.toString();
				record.category = category;
				record.message = message;
				record.expression = expr.toString();
				record.in = in;
				record.expected = toSummary(expected);
				record.actual = toSummary(actual);
				record.testCase = testCase;

				System.err.println(MAPPER.writeValueAsString(record));
				diagnostics.add(record);
			} else {
				passedCount++;
				if (expected.error == null && !expected.values.isEmpty()) {
					actual.values.forEach(v -> {
						if (uniqueValues.add(v)) {
							values.add(v);
							expressions.add(new RawJsonValue(v));
						}
					});
					expressions.add(expr);
				}
			}
		}

		SummaryRecord summary = new SummaryRecord();
		summary.totalIterations = iterations;
		summary.passed = passedCount;
		summary.errors = diagnostics.size();
		summary.seed = seed;
		summary.version = version.toString();
		Map<Category, Integer> categoryCounts = new LinkedHashMap<>();
		for (DiagnosticRecord d : diagnostics)
			categoryCounts.merge(d.category, 1, Integer::sum);
		summary.categories = categoryCounts;

		System.err.println(MAPPER.writeValueAsString(summary));

		if (failOnError && !diagnostics.isEmpty()) {
			System.err.println(String.format("jackson-jq-fuzz found %d error(s) across %d iterations.", diagnostics.size(), iterations));
			System.exit(1);
		}
	}

	/**
	 * Wraps a previously-computed constant value (e.g. an object or array) so it can be re-fed into
	 * later generators, formatted the same way {@link JsonNode#toString()}
	 * already renders it -- which happens to be valid jq literal syntax.
	 */
	private static class RawJsonValue implements AstNode {
		private final JsonNode value;

		RawJsonValue(JsonNode value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TestCase {
		@JsonProperty("v")
		@JsonSerialize(using = ToStringSerializer.class)
		public @Nullable VersionRange version;

		@JsonProperty("q")
		@JsonSerialize(using = ToStringSerializer.class)
		public @Nullable AstNode expression;

		@JsonProperty("in")
		public @Nullable JsonNode in;

		@JsonProperty("out")
		public @Nullable List<JsonNode> out;

		@Override
		public String toString() {
			return String.format("jq '%s' <<< '%s' # should be %s, version = %s.", expression, in, out, version != null ? version : "any");
		}
	}
}
