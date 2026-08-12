package net.thisptr.jackson.jq.v2.test.random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.VersionRange;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.tree.ArrayConstruction;
import net.thisptr.jackson.jq.v2.core.internal.tree.FunctionCall;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.core.internal.tree.TryCatch;
import net.thisptr.jackson.jq.v2.core.internal.tree.Tuple;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.AlternativeOperatorExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanAndExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BooleanOrExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.DivideExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MinusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.ModuloExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.MultiplyExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.PlusExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketExtractFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess.BracketFieldAccess;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.BooleanLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.DoubleLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.LongLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator.Result;
import net.thisptr.jackson.jq.v2.test.evaluator.JacksonJqEvaluator;
import net.thisptr.jackson.jq.v2.test.evaluator.TrueJqEvaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// -DrandomTests=true
@EnabledIfSystemProperty(named = "randomTests", matches = "true")
public class RandomTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Version VERSION = Versions.JQ_1_5;

	private static Map<Version, Set<String>> EXCLUDED_FUNCTIONS = new HashMap<>();
	static {
		EXCLUDED_FUNCTIONS.computeIfAbsent(Versions.JQ_1_5, k -> {
			return new HashSet<>(Arrays.asList(new String[] {
					"debug_scope/0", // a debug function for jackson-jq
					"log2/0", // log2 has slightly different precisions
			}));
		});
		EXCLUDED_FUNCTIONS.computeIfAbsent(Versions.JQ_1_6, k -> {
			return new HashSet<>(Arrays.asList(new String[] {
					"debug_scope/0", // a debug function for jackson-jq
					"log2/0", // log2 has slightly different precisions
			}));
		});
	}

	private static List<Generator> GENERATORS = new ArrayList<>();

	private List<Expression<JsonNode>> expressions = new ArrayList<>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@BeforeAll
	static void beforeAll() {
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new PlusExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new MinusExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new ModuloExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BooleanAndExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BooleanOrExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new AlternativeOperatorExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new Tuple(exprs)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new ArrayConstruction(exprs.get(0))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new MultiplyExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new DivideExpression(exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(0, (exprs) -> new ThisObject()));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccess(exprs.get(0), exprs.get(1), true)));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccess(exprs.get(0), exprs.get(1), false)));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccess(exprs.get(0), exprs.get(1), exprs.get(2), true)));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccess(exprs.get(0), exprs.get(1), exprs.get(2), false)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccess(exprs.get(0), true)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccess(exprs.get(0), false)));

		Set<String> exclusions = EXCLUDED_FUNCTIONS.getOrDefault(VERSION, Collections.emptySet());
		BuiltinFunctionLoader.getInstance().listFunctions(Scope.class.getClassLoader(), VERSION, Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance())).forEach((signature, function) -> {
			if (exclusions.contains(signature))
				return;
			if (signature.contains("/")) {
				int numArgs = Integer.parseInt(signature.split("/", 2)[1]);
				String name = signature.split("/", 2)[0];
				if (exclusions.contains(name))
					return;
				GENERATORS.add(new RandomGenerator(numArgs, (exprs) -> new FunctionCall(null, name, (List) exprs, VERSION)));
			} else {
				GENERATORS.add(new RandomGenerator(0, 10, (exprs) -> new FunctionCall(null, signature, (List) exprs, VERSION)));
			}
		});
	}

	@BeforeEach
	void beforeEach() {
		expressions.add(new BooleanLiteral<>(true));
		expressions.add(new BooleanLiteral<>(false));
		expressions.add(new LongLiteral<>(-1));
		expressions.add(new LongLiteral<>(0));
		expressions.add(new LongLiteral<>(1));
		expressions.add(new DoubleLiteral<>(-1.5));
		expressions.add(new DoubleLiteral<>(-1.0));
		expressions.add(new DoubleLiteral<>(-1.0));
		expressions.add(new DoubleLiteral<>(-0.5));
		expressions.add(new DoubleLiteral<>(0.0));
		expressions.add(new DoubleLiteral<>(0.5));
		expressions.add(new DoubleLiteral<>(1.0));
		expressions.add(new DoubleLiteral<>(1.5));
		expressions.add(new NullLiteral<>());
		expressions.add(new StringLiteral<>("foo"));
		expressions.add(new StringLiteral<>("bar"));
		expressions.add(new StringLiteral<>("baz"));
		expressions.add(new EmptyExpression());
		expressions.add(new StringLiteral<>("\r"));
		expressions.add(new StringLiteral<>("\n"));
		expressions.add(new StringLiteral<>("\t"));
		expressions.add(new StringLiteral<>("\0"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testRandom() throws Throwable {
		List<JsonNode> values = new ArrayList<>();
		Set<JsonNode> uniqueValues = new TreeSet<>(new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance()));

		values.add(NullNode.getInstance());
		uniqueValues.add(NullNode.getInstance());

		Random random = new Random();

		for (int i = 0; i < 10000; ++i) {
			Generator generator = GENERATORS.get(random.nextInt(GENERATORS.size()));

			List<Expression<JsonNode>> args = new ArrayList<>();
			for (int j = 0; j < generator.args(); ++j)
				args.add(expressions.get(random.nextInt(expressions.size())));

			@SuppressWarnings({ "unchecked", "rawtypes" })
			Expression<JsonNode> expr = generator.generate((List) args);
			// System.out.println(expr);

			JsonNode in = values.get(random.nextInt(values.size()));

			Result expected;
			try {
				expected = new TrueJqEvaluator().evaluate(expr.toString(), in, VERSION, 1000L);
			} catch (Throwable e) {
				// System.err.printf("Cloud not evaluate jq '%s' <<< '%s'%n", expr, in);
				continue;
			}

			Result actual;
			try {
				actual = new JacksonJqEvaluator().evaluate(expr.toString(), in, VERSION, 1000L);
			} catch (Throwable e) {
				// System.err.printf("Cloud not evaluate jackson-jq '%s' <<< '%s'%n", expr, in);
				continue;
			}

			try {
				assertEquals(expected.error != null, actual.error != null, "one failed with an error and the other succeeded: expected = " + expected.error + ", actual = " + actual.error);
				// TODO: compare error message

				assertEquals(expected.values.size(), actual.values.size(), "the number of output value doesn't match");
				for (int t = 0; t < expected.values.size(); ++t) {
					if (new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance()).compare(expected.values.get(t), actual.values.get(t)) == 0)
						continue;
					fail("Expected: " + expected.values.get(t) + ", Actual: " + actual.values.get(t) + ".");
				}

				if (expected.error == null && !expected.values.isEmpty()) {
					actual.values.forEach(v -> {
						if (uniqueValues.add(v)) {
							values.add(v);
							expressions.add(new LiteralExpression(v));
							// System.out.printf("Added %s%n", v);
						}
					});
					expressions.add(expr);
				}
			} catch (Throwable th) {
				TestCase test = new TestCase();
				test.in = in;
				test.version = new VersionRange(VERSION, true, VERSION, true);
				if (expected.error != null) {
					test.expression = new TryCatch<>(expr, new StringLiteral<>("__ERROR__"));
					test.out = new ArrayList<>(expected.values);
					test.out.add(TextNode.valueOf("__ERROR__"));
				} else {
					test.expression = expr;
					test.out = expected.values;
				}
				System.err.println("# " + MAPPER.writeValueAsString(test));
				System.err.printf("$ jq '%s' <<< '%s' # version = %s%n", expr, test.in, test.version != null ? test.version : "");
				for (JsonNode out : expected.values)
					System.err.printf("%s%n", out);
				if (expected.error != null) {
					if (!(expected.error instanceof JsonQueryException))
						throw expected.error;
					System.err.printf("jq: error (at <unknown>): %s%n", String.valueOf(expected.error.getMessage()).replace("\n", "\\n"));
				}
				System.err.printf("$ jackson-jq '%s' <<< '%s' # version = %s%n", expr, test.in, test.version != null ? test.version : "");
				for (JsonNode out : actual.values)
					System.err.printf("%s%n", out);
				if (actual.error != null) {
					if (!(actual.error instanceof JsonQueryException))
						throw actual.error;
					System.err.printf("jq: error (at <unknown>): %s%n", String.valueOf(actual.error.getMessage()).replace("\n", "\\n"));
				}
				System.err.printf("---%n");
				// throw th;
			}
		}
	}

	public static class TestCase {
		@JsonProperty("v")
		@JsonSerialize(using = ToStringSerializer.class)
		public @Nullable VersionRange version;

		@JsonProperty("q")
		@JsonSerialize(using = ToStringSerializer.class)
		public @Nullable Expression<JsonNode> expression;

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
