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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
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

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ArrayMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BooleanLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketExtractFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BracketFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BreakExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ConditionalAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ForeachExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FormattingFilterAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionCallAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.IdentifierFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NegativeExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NullLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.NumericLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectConstructionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ObjectMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipedQueryAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.RecursionOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ReduceExpressionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringFieldAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TryCatchAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ValueMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableAccessAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;
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
			"builtins/0", // the set of built-in functions inherently differs between jq and jackson-jq
			"_match_impl/3", // internal regex implementation function
			"_match_impl"
	));

	// The fuzzer assembles ASTs directly rather than parsing them, so its nodes have no real
	// source text to point at.
	private static final SourceLocation SYNTHETIC = SourceLocation.of(1, 1);

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

			// Conditionals (if-then-else, if-then-elif-then-else)
			generators.add(new RandomGenerator(3, (exprs) -> new ConditionalAstNode(SYNTHETIC, Collections.singletonList(Pair.of(exprs.get(0), exprs.get(1))), exprs.get(2))));
			generators.add(new RandomGenerator(5, (exprs) -> new ConditionalAstNode(SYNTHETIC, Arrays.asList(Pair.of(exprs.get(0), exprs.get(1)), Pair.of(exprs.get(2), exprs.get(3))), exprs.get(4))));

			// Try-Catch & Question Operator
			generators.add(new RandomGenerator(2, (exprs) -> new TryCatchAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(1, (exprs) -> new TryCatchAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(1, (exprs) -> new TryCatchAstNode.Question(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)))));

			// Parentheses & Unary Negation
			generators.add(new RandomGenerator(1, (exprs) -> new ParenAstNode(SYNTHETIC, exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new NegativeExpressionAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)))));

			// Recursion Operator
			generators.add(new RandomGenerator(0, (exprs) -> new RecursionOperatorAstNode(SYNTHETIC)));

			// Binary Operators - Arithmetic
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.MINUS, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.MODULO, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.TIMES, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.DIVIDE, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));

			// Binary Operators - Logical & Alternative
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.AND, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.OR, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.DEFAULT, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));

			// Binary Operators - Comparison
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.EQUAL, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.NOT_EQUAL, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.LESS, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.LESS_EQUAL, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.GREATER, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.GREATER_EQUAL, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ParenAstNode(SYNTHETIC, exprs.get(1)))));

			// Binary Operators - Assignment & Update with path expression LHS
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.ASSIGN, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.UPDATE, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.DEFAULT_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.MINUS_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.TIMES_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.DIVIDE_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.MODULO_EQUAL, new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.ASSIGN, new BracketExtractFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.UPDATE, new BracketExtractFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.ASSIGN, new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), new NumericLiteralAstNode(SYNTHETIC, "0"), false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.ASSIGN, new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), new NumericLiteralAstNode(SYNTHETIC, "0"), new NumericLiteralAstNode(SYNTHETIC, "1"), false), exprs.get(0))));
			generators.add(new RandomGenerator(1, (exprs) -> new BinaryOpAstNode(SYNTHETIC, BinaryOperator.ASSIGN, new StringFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), new StringLiteralAstNode(SYNTHETIC, "bar"), false), exprs.get(0))));

			// Pipelines & Variable Bindings
			generators.add(new RandomGenerator(2, (exprs) -> new PipedQueryAstNode(SYNTHETIC, exprs.get(0), exprs.get(1))));
			generators.add(new RandomGenerator(3, (exprs) -> new PipedQueryAstNode(SYNTHETIC, exprs.get(0), new PipedQueryAstNode(SYNTHETIC, exprs.get(1), exprs.get(2)))));
			generators.add(new RandomGenerator(2, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1))));
			generators.add(new RandomGenerator(1, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ValueMatcherAstNode(SYNTHETIC, "x"), new VariableAccessAstNode(SYNTHETIC, "x"))));
			generators.add(new RandomGenerator(2, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ValueMatcherAstNode(SYNTHETIC, "x"), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new VariableAccessAstNode(SYNTHETIC, "x"), exprs.get(1)))));
			generators.add(new RandomGenerator(2, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ValueMatcherAstNode(SYNTHETIC, "x"), new ArrayConstructionAstNode(SYNTHETIC, new TupleAstNode(SYNTHETIC, Arrays.asList(new VariableAccessAstNode(SYNTHETIC, "x"), exprs.get(1)))))));
			generators.add(new RandomGenerator(3, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ValueMatcherAstNode(SYNTHETIC, "x"), new ConditionalAstNode(SYNTHETIC, Collections.singletonList(Pair.of(new VariableAccessAstNode(SYNTHETIC, "x"), exprs.get(1))), exprs.get(2)))));
			generators.add(new RandomGenerator(1, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ArrayMatcherAstNode(SYNTHETIC, Arrays.asList(new ValueMatcherAstNode(SYNTHETIC, "a"), new ValueMatcherAstNode(SYNTHETIC, "b"))), new ArrayConstructionAstNode(SYNTHETIC, new TupleAstNode(SYNTHETIC, Arrays.asList(new VariableAccessAstNode(SYNTHETIC, "b"), new VariableAccessAstNode(SYNTHETIC, "a")))))));
			generators.add(new RandomGenerator(1, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ObjectMatcherAstNode(SYNTHETIC, Arrays.asList(new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, true, "a", null), new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, true, "b", null))), new ArrayConstructionAstNode(SYNTHETIC, new TupleAstNode(SYNTHETIC, Arrays.asList(new VariableAccessAstNode(SYNTHETIC, "a"), new VariableAccessAstNode(SYNTHETIC, "b")))))));
			generators.add(new RandomGenerator(1, (exprs) -> new VariableBindingAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new ObjectMatcherAstNode(SYNTHETIC, Arrays.asList(new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, false, "foo", new ValueMatcherAstNode(SYNTHETIC, "a")), new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, false, "bar", new ValueMatcherAstNode(SYNTHETIC, "b")))), new ArrayConstructionAstNode(SYNTHETIC, new TupleAstNode(SYNTHETIC, Arrays.asList(new VariableAccessAstNode(SYNTHETIC, "a"), new VariableAccessAstNode(SYNTHETIC, "b")))))));
			generators.add(new RandomGenerator(3, (exprs) -> new LabelAstNode(SYNTHETIC, "out", new TupleAstNode(SYNTHETIC, Arrays.asList(exprs.get(0), new ConditionalAstNode(SYNTHETIC, Collections.singletonList(Pair.of(exprs.get(1), new BreakExpressionAstNode(SYNTHETIC, "out"))), exprs.get(2)))))));

			// Reduce & Foreach
			generators.add(new RandomGenerator(3, (exprs) -> new ReduceExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), exprs.get(2), new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(2, (exprs) -> new ReduceExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ThisObjectAstNode(SYNTHETIC), new VariableAccessAstNode(SYNTHETIC, "x")), new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(2, (exprs) -> new ReduceExpressionAstNode(SYNTHETIC, new ArrayMatcherAstNode(SYNTHETIC, Arrays.asList(new ValueMatcherAstNode(SYNTHETIC, "a"), new ValueMatcherAstNode(SYNTHETIC, "b"))), exprs.get(1), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ThisObjectAstNode(SYNTHETIC), new VariableAccessAstNode(SYNTHETIC, "a")), new VariableAccessAstNode(SYNTHETIC, "b")), new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(2, (exprs) -> new ReduceExpressionAstNode(SYNTHETIC, new ObjectMatcherAstNode(SYNTHETIC, Arrays.asList(new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, true, "a", null), new ObjectMatcherAstNode.ConstantKeyFieldMatcher(SYNTHETIC, true, "b", null))), exprs.get(1), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ThisObjectAstNode(SYNTHETIC), new VariableAccessAstNode(SYNTHETIC, "a")), new VariableAccessAstNode(SYNTHETIC, "b")), new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(3, (exprs) -> new ForeachExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), exprs.get(2), null, new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(2, (exprs) -> new ForeachExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ThisObjectAstNode(SYNTHETIC), new VariableAccessAstNode(SYNTHETIC, "x")), null, new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(4, (exprs) -> new ForeachExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), exprs.get(2), exprs.get(3), new ParenAstNode(SYNTHETIC, exprs.get(0)))));
			generators.add(new RandomGenerator(2, (exprs) -> new ForeachExpressionAstNode(SYNTHETIC, new ValueMatcherAstNode(SYNTHETIC, "x"), exprs.get(1), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.PLUS, new ThisObjectAstNode(SYNTHETIC), new VariableAccessAstNode(SYNTHETIC, "x")), new BinaryOpAstNode(SYNTHETIC, BinaryOperator.TIMES, new ThisObjectAstNode(SYNTHETIC), new NumericLiteralAstNode(SYNTHETIC, "2")), new ParenAstNode(SYNTHETIC, exprs.get(0)))));

			// Object Construction
			generators.add(new RandomGenerator(0, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Collections.emptyList())));
			generators.add(new RandomGenerator(1, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst(SYNTHETIC, "foo", new ParenAstNode(SYNTHETIC, exprs.get(0)))))));
			generators.add(new RandomGenerator(0, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst(SYNTHETIC, "foo")))));
			generators.add(new RandomGenerator(1, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.StringKeyFieldConstructionAst(SYNTHETIC, new StringLiteralAstNode(SYNTHETIC, "foo"), new ParenAstNode(SYNTHETIC, exprs.get(0)))))));
			generators.add(new RandomGenerator(0, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.StringKeyFieldConstructionAst(SYNTHETIC, new StringLiteralAstNode(SYNTHETIC, "foo"))))));
			generators.add(new RandomGenerator(2, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst(SYNTHETIC, exprs.get(0), new ParenAstNode(SYNTHETIC, exprs.get(1)))))));
			generators.add(new RandomGenerator(2, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst(SYNTHETIC, "foo", new ParenAstNode(SYNTHETIC, exprs.get(0))), new ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst(SYNTHETIC, "bar", new ParenAstNode(SYNTHETIC, exprs.get(1)))))));
			generators.add(new RandomGenerator(2, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.StringKeyFieldConstructionAst(SYNTHETIC, new StringLiteralAstNode(SYNTHETIC, "foo"), new ParenAstNode(SYNTHETIC, exprs.get(0))), new ObjectConstructionAstNode.StringKeyFieldConstructionAst(SYNTHETIC, new StringLiteralAstNode(SYNTHETIC, "bar"), new ParenAstNode(SYNTHETIC, exprs.get(1)))))));
			generators.add(new RandomGenerator(4, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst(SYNTHETIC, exprs.get(0), new ParenAstNode(SYNTHETIC, exprs.get(1))), new ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst(SYNTHETIC, exprs.get(2), new ParenAstNode(SYNTHETIC, exprs.get(3)))))));
			generators.add(new RandomGenerator(3, (exprs) -> new ObjectConstructionAstNode(SYNTHETIC, Arrays.asList(new ObjectConstructionAstNode.IdentifierKeyFieldConstructionAst(SYNTHETIC, "foo", new ParenAstNode(SYNTHETIC, exprs.get(0))), new ObjectConstructionAstNode.JsonQueryKeyFieldConstructionAst(SYNTHETIC, exprs.get(1), new ParenAstNode(SYNTHETIC, exprs.get(2)))))));

			// Array Construction & Tuples
			generators.add(new RandomGenerator(0, (exprs) -> new ArrayConstructionAstNode(SYNTHETIC)));
			generators.add(new RandomGenerator(1, (exprs) -> new ArrayConstructionAstNode(SYNTHETIC, exprs.get(0))));
			generators.add(new RandomGenerator(2, (exprs) -> new TupleAstNode(SYNTHETIC, exprs)));
			generators.add(new RandomGenerator(3, (exprs) -> new TupleAstNode(SYNTHETIC, exprs)));

			// This Object & Field Access
			generators.add(new RandomGenerator(0, (exprs) -> new ThisObjectAstNode(SYNTHETIC)));
			generators.add(new RandomGenerator(0, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", true)));
			generators.add(new RandomGenerator(0, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "foo", false)));
			generators.add(new RandomGenerator(0, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "bar", true)));
			generators.add(new RandomGenerator(0, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), "bar", false)));
			generators.add(new RandomGenerator(1, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), "foo", true)));
			generators.add(new RandomGenerator(1, (exprs) -> new IdentifierFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), "foo", false)));
			generators.add(new RandomGenerator(0, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), new StringLiteralAstNode(SYNTHETIC, "foo"), true)));
			generators.add(new RandomGenerator(0, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), new StringLiteralAstNode(SYNTHETIC, "foo"), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new StringLiteralAstNode(SYNTHETIC, "foo"), true)));
			generators.add(new RandomGenerator(1, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), new StringLiteralAstNode(SYNTHETIC, "foo"), false)));
			generators.add(new RandomGenerator(2, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), true)));
			generators.add(new RandomGenerator(2, (exprs) -> new StringFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), true)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), false)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), true)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), false)));
			generators.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), exprs.get(2), true)));
			generators.add(new RandomGenerator(3, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), exprs.get(2), false)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), exprs.get(1), true)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), exprs.get(1), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), null, exprs.get(0), true)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), null, exprs.get(0), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), null, true)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), exprs.get(0), null, false)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), null, exprs.get(1), true)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), null, exprs.get(1), false)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), null, true)));
			generators.add(new RandomGenerator(2, (exprs) -> new BracketFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), exprs.get(1), null, false)));
			generators.add(new RandomGenerator(0, (exprs) -> new BracketExtractFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), true)));
			generators.add(new RandomGenerator(0, (exprs) -> new BracketExtractFieldAccessAstNode(SYNTHETIC, new ThisObjectAstNode(SYNTHETIC), false)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), true)));
			generators.add(new RandomGenerator(1, (exprs) -> new BracketExtractFieldAccessAstNode(SYNTHETIC, new ParenAstNode(SYNTHETIC, exprs.get(0)), false)));

			// String Interpolation & Formatting Filters
			generators.add(new RandomGenerator(1, (exprs) -> new StringInterpolationAstNode(SYNTHETIC, "foo  bar", Collections.singletonList(Pair.of(4, exprs.get(0))), null)));
			generators.add(new RandomGenerator(1, (exprs) -> new StringInterpolationAstNode(SYNTHETIC, "", Collections.singletonList(Pair.of(0, exprs.get(0))), null)));
			generators.add(new RandomGenerator(2, (exprs) -> new StringInterpolationAstNode(SYNTHETIC, " - ", Arrays.asList(Pair.of(0, exprs.get(0)), Pair.of(3, exprs.get(1))), null)));
			generators.add(new RandomGenerator(1, (exprs) -> new StringInterpolationAstNode(SYNTHETIC, "foo ", Collections.singletonList(Pair.of(4, exprs.get(0))), new FormattingFilterAstNode(SYNTHETIC, "uri"))));
			generators.add(new RandomGenerator(1, (exprs) -> new StringInterpolationAstNode(SYNTHETIC, "foo ", Collections.singletonList(Pair.of(4, exprs.get(0))), new FormattingFilterAstNode(SYNTHETIC, "json"))));

			for (String fmt : Arrays.asList("base64", "json", "uri", "csv", "tsv", "sh", "html", "text")) {
				generators.add(new RandomGenerator(0, (exprs) -> new FormattingFilterAstNode(SYNTHETIC, fmt)));
			}
			if (version.compareTo(Versions.JQ_1_6) >= 0) {
				generators.add(new RandomGenerator(0, (exprs) -> new FormattingFilterAstNode(SYNTHETIC, "base64d")));
			}

			// User-Defined Functions
			generators.add(new RandomGenerator(1, (exprs) -> new SemicolonOperatorAstNode(SYNTHETIC, Arrays.asList(
					new FunctionDefinitionAstNode(SYNTHETIC, "f", Collections.emptyList(), exprs.get(0)),
					new FunctionCallAstNode(SYNTHETIC, null, "f", Collections.emptyList())))));
			generators.add(new RandomGenerator(2, (exprs) -> new SemicolonOperatorAstNode(SYNTHETIC, Arrays.asList(
					new FunctionDefinitionAstNode(SYNTHETIC, "f", Collections.emptyList(), exprs.get(0)),
					new PipedQueryAstNode(SYNTHETIC, exprs.get(1), new FunctionCallAstNode(SYNTHETIC, null, "f", Collections.emptyList()))))));
			generators.add(new RandomGenerator(2, (exprs) -> new SemicolonOperatorAstNode(SYNTHETIC, Arrays.asList(
					new FunctionDefinitionAstNode(SYNTHETIC, "f", Collections.singletonList("a"), exprs.get(0)),
					new FunctionCallAstNode(SYNTHETIC, null, "f", Collections.singletonList(exprs.get(1)))))));
			generators.add(new RandomGenerator(3, (exprs) -> new SemicolonOperatorAstNode(SYNTHETIC, Arrays.asList(
					new FunctionDefinitionAstNode(SYNTHETIC, "f", Collections.singletonList("a"), exprs.get(0)),
					new PipedQueryAstNode(SYNTHETIC, exprs.get(1), new FunctionCallAstNode(SYNTHETIC, null, "f", Collections.singletonList(exprs.get(2))))))));
			generators.add(new RandomGenerator(3, (exprs) -> new SemicolonOperatorAstNode(SYNTHETIC, Arrays.asList(
					new FunctionDefinitionAstNode(SYNTHETIC, "f", Arrays.asList("a", "b"), exprs.get(0)),
					new FunctionCallAstNode(SYNTHETIC, null, "f", Arrays.asList(exprs.get(1), exprs.get(2)))))));

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
					generators.add(new RandomGenerator(numArgs, (exprs) -> new FunctionCallAstNode(SYNTHETIC, null, name, exprs)));
				} else {
					generators.add(new RandomGenerator(0, 10, (exprs) -> new FunctionCallAstNode(SYNTHETIC, null, signature, exprs)));
				}
			});
			return generators;
		});
	}

	private static List<AstNode> createInitialExpressions(Version version) {
		List<AstNode> expressions = new ArrayList<>();
		expressions.add(new ThisObjectAstNode(SYNTHETIC));
		expressions.add(new RecursionOperatorAstNode(SYNTHETIC));
		expressions.add(new ArrayConstructionAstNode(SYNTHETIC));
		expressions.add(new ObjectConstructionAstNode(SYNTHETIC, Collections.emptyList()));
		expressions.add(new BooleanLiteralAstNode(SYNTHETIC, true));
		expressions.add(new BooleanLiteralAstNode(SYNTHETIC, false));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "-2"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "-1"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "0"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "1"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "2"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "10"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "-1.5"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "-1.0"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "-0.5"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "0.0"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "0.5"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "1.0"));
		expressions.add(new NumericLiteralAstNode(SYNTHETIC, "1.5"));
		expressions.add(new NullLiteralAstNode(SYNTHETIC));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, ""));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "foo"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "bar"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "baz"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "0"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "true"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "null"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "a"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "name"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "value"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "{}"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "[]"));
		expressions.add(new FunctionCallAstNode(SYNTHETIC, null, "empty", Collections.emptyList()));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "\r"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "\n"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "\t"));
		expressions.add(new StringLiteralAstNode(SYNTHETIC, "\0"));
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
		for (JsonNode initialVal : Arrays.asList(
				NullNode.getInstance(),
				BooleanNode.TRUE,
				BooleanNode.FALSE,
				IntNode.valueOf(0),
				IntNode.valueOf(1),
				IntNode.valueOf(-1),
				TextNode.valueOf(""),
				TextNode.valueOf("foo"),
				TextNode.valueOf("bar"),
				MAPPER.createObjectNode(),
				MAPPER.createArrayNode(),
				MAPPER.readTree("{\"foo\": 1, \"bar\": \"baz\"}"),
				MAPPER.readTree("[1, 2, 3]"))) {
			if (uniqueValues.add(initialVal))
				values.add(initialVal);
		}

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
					testCase.expression = new TryCatchAstNode(SYNTHETIC, expr, new StringLiteralAstNode(SYNTHETIC, "__ERROR__"));
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
							expressions.add(toAstNode(v));
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

	private static AstNode toAstNode(JsonNode value) {
		if (value.isNull())
			return new NullLiteralAstNode(SYNTHETIC);
		if (value.isBoolean())
			return new BooleanLiteralAstNode(SYNTHETIC, value.booleanValue());
		if (value.isNumber())
			return new NumericLiteralAstNode(SYNTHETIC, value.toString());
		if (value.isTextual())
			return new StringLiteralAstNode(SYNTHETIC, value.textValue());
		if (value.isArray()) {
			List<AstNode> elements = new ArrayList<>();
			for (JsonNode element : value)
				elements.add(toAstNode(element));
			return elements.isEmpty()
					? new ArrayConstructionAstNode(SYNTHETIC)
					: new ArrayConstructionAstNode(SYNTHETIC, new TupleAstNode(SYNTHETIC, elements));
		}
		if (value.isObject()) {
			List<ObjectConstructionAstNode.FieldConstructionAst> fields = new ArrayList<>();
			value.properties().forEach(field -> fields.add(
					new ObjectConstructionAstNode.StringKeyFieldConstructionAst(SYNTHETIC,
							new StringLiteralAstNode(SYNTHETIC, field.getKey()), toAstNode(field.getValue()))));
			return new ObjectConstructionAstNode(SYNTHETIC, fields);
		}
		throw new IllegalArgumentException("Unsupported JSON value: " + value);
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
