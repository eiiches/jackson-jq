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
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;
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
					"log2/0", // log2 has slightly different precisions
			}));
		});
		EXCLUDED_FUNCTIONS.computeIfAbsent(Versions.JQ_1_6, k -> {
			return new HashSet<>(Arrays.asList(new String[] {
					"log2/0", // log2 has slightly different precisions
			}));
		});
	}

	private static List<Generator> GENERATORS = new ArrayList<>();

	private List<AstNode> expressions = new ArrayList<>();

	@BeforeAll
	static void beforeAll() {
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.PLUS, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.MINUS, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.MODULO, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.AND, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.OR, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.DEFAULT, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new TupleAstNode(exprs)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new ArrayConstructionAstNode(exprs.get(0))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.TIMES, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(BinaryOperatorExpression.Operator.DIVIDE, exprs.get(0), exprs.get(1))));
		GENERATORS.add(new RandomGenerator(0, (exprs) -> new ThisObjectAstNode()));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), true)));
		GENERATORS.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), false)));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), exprs.get(2), true)));
		GENERATORS.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(exprs.get(0), exprs.get(1), exprs.get(2), false)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(exprs.get(0), true)));
		GENERATORS.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(exprs.get(0), false)));

		Set<String> exclusions = EXCLUDED_FUNCTIONS.getOrDefault(VERSION, Collections.emptySet());
		BuiltinFunctionLoader.getInstance().listFunctionFactories(VERSION).forEach((nameAndArity, factory) -> {
			String signature = nameAndArity.toString();
			if (exclusions.contains(signature))
				return;
			if (signature.contains("/")) {
				int numArgs = Integer.parseInt(signature.split("/", 2)[1]);
				String name = signature.split("/", 2)[0];
				if (exclusions.contains(name))
					return;
				GENERATORS.add(new RandomGenerator(numArgs, (exprs) -> new FunctionCallAstNode(null, name, exprs, VERSION)));
			} else {
				GENERATORS.add(new RandomGenerator(0, 10, (exprs) -> new FunctionCallAstNode(null, signature, exprs, VERSION)));
			}
		});
	}

	@BeforeEach
	void beforeEach() {
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
		expressions.add(new FunctionCallAstNode(null, "empty", Collections.emptyList(), VERSION));
		expressions.add(new StringLiteralAstNode("\r"));
		expressions.add(new StringLiteralAstNode("\n"));
		expressions.add(new StringLiteralAstNode("\t"));
		expressions.add(new StringLiteralAstNode("\0"));
	}

	@Test
	void testRandom() throws Throwable {
		List<JsonNode> values = new ArrayList<>();
		Set<JsonNode> uniqueValues = new TreeSet<>(new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance()));

		values.add(NullNode.getInstance());
		uniqueValues.add(NullNode.getInstance());

		Random random = new Random();

		for (int i = 0; i < 10000; ++i) {
			Generator generator = GENERATORS.get(random.nextInt(GENERATORS.size()));

			List<AstNode> args = new ArrayList<>();
			for (int j = 0; j < generator.args(); ++j)
				args.add(expressions.get(random.nextInt(expressions.size())));

			AstNode expr = generator.generate(args);
			// System.out.println(expr);

			JsonNode in = values.get(random.nextInt(values.size()));

			Evaluator.Result expected;
			try {
				expected = new TrueJqEvaluator().evaluate(expr.toString(), in, VERSION, 1000L);
			} catch (Throwable e) {
				// System.err.printf("Cloud not evaluate jq '%s' <<< '%s'%n", expr, in);
				continue;
			}

			Evaluator.Result actual;
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
							expressions.add(new RawJsonValue(v));
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
					test.expression = new TryCatchAstNode(expr, new StringLiteralAstNode("__ERROR__"));
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

	/**
	 * Wraps a previously-computed constant value (e.g. an object or array) so it can be re-fed into
	 * later generators, formatted the same way {@link com.fasterxml.jackson.databind.JsonNode#toString()}
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
