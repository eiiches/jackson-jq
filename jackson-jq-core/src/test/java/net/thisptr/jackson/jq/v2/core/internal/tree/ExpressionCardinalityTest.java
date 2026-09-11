package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionCardinalityTest {

	@Test
	public void testCardinalityUtilsMultiply() {
		List<Cardinality> empty = Collections.emptyList();
		assertThat(CardinalityUtils.multiply(empty, (Cardinality c) -> c)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.multiply(Cardinality.ONE, Cardinality.ONE)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.multiply(Cardinality.ONE, Cardinality.ONE, Cardinality.ONE)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.multiply(Cardinality.ZERO, Cardinality.ONE)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.multiply(Cardinality.ONE, Cardinality.ZERO)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.multiply(Cardinality.ZERO, Cardinality.UNKNOWN)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.multiply(Cardinality.UNKNOWN, Cardinality.ZERO)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.multiply(Cardinality.ONE, Cardinality.UNKNOWN)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.multiply(Cardinality.UNKNOWN, Cardinality.ONE)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.multiply(Cardinality.UNKNOWN, Cardinality.UNKNOWN)).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testCardinalityUtilsSum() {
		List<Cardinality> empty = Collections.emptyList();
		assertThat(CardinalityUtils.sum(empty, (Cardinality c) -> c)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.sum(Collections.singletonList(Cardinality.ONE), (Cardinality c) -> c)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.sum(Collections.singletonList(Cardinality.ZERO), (Cardinality c) -> c)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.sum(Collections.singletonList(Cardinality.UNKNOWN), (Cardinality c) -> c)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ZERO, Cardinality.ZERO), (Cardinality c) -> c)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ZERO, Cardinality.ONE), (Cardinality c) -> c)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ONE, Cardinality.ZERO), (Cardinality c) -> c)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ONE, Cardinality.ONE), (Cardinality c) -> c)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ZERO, Cardinality.UNKNOWN), (Cardinality c) -> c)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.sum(Arrays.asList(Cardinality.ONE, Cardinality.UNKNOWN), (Cardinality c) -> c)).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testCardinalityUtilsAlternative() {
		assertThat(CardinalityUtils.alternative(Cardinality.ONE, Cardinality.ONE)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.alternative(Cardinality.ONE, Cardinality.ZERO)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.alternative(Cardinality.ONE, Cardinality.UNKNOWN)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.alternative(Cardinality.ZERO, Cardinality.ZERO)).isEqualTo(Cardinality.ZERO);
		assertThat(CardinalityUtils.alternative(Cardinality.ZERO, Cardinality.ONE)).isEqualTo(Cardinality.ONE);
		assertThat(CardinalityUtils.alternative(Cardinality.ZERO, Cardinality.UNKNOWN)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.alternative(Cardinality.UNKNOWN, Cardinality.ONE)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.alternative(Cardinality.UNKNOWN, Cardinality.ZERO)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(CardinalityUtils.alternative(Cardinality.UNKNOWN, Cardinality.UNKNOWN)).isEqualTo(Cardinality.UNKNOWN);
	}

	private static Cardinality cardinalityOf(String expression) {
		return cardinalityOf(expression, Versions.JQ_1_7);
	}

	private static Cardinality cardinalityOf(String expression, Version jqVersion) {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), jqVersion)
				.build();
		AstNode parsedAst = AstParser.parse(expression, env.getJqVersion());
		Expression<StackFrame, JsonNode> compiledExpr = Compiler.compile(env, (Module) null, parsedAst);
		return compiledExpr.getCardinality();
	}

	@Test
	public void testLiterals() {
		assertThat(cardinalityOf("1")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("\"hello\"")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("true")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("false")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("null")).isEqualTo(Cardinality.ONE);
	}

	@Test
	public void testConstructors() {
		assertThat(cardinalityOf("[]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("[1, 2, 3]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("[.[]]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("{}")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("{a: 1, b: 2}")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("{\"a\": 1, \"b\": 2}")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("{a: (1, 2)}")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("{a: empty}")).isEqualTo(Cardinality.ZERO);
	}

	@Test
	public void testIdentityAndControlFlow() {
		assertThat(cardinalityOf(".")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("empty")).isEqualTo(Cardinality.ZERO);
		assertThat(new BreakExpression<JsonNode>("out").getCardinality()).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("label $out | break $out")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("label $out | (1, break $out)")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("-1")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("-empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("-(1, 2)")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testPipedQuery() {
		assertThat(cardinalityOf("1 | 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 | empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("empty | 1")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("(1, 2) | .")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("1 | (2, 3)")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testSemicolonOperator() {
		assertThat(new SemicolonOperator<JsonNode>(Collections.emptyList()).getCardinality()).isEqualTo(Cardinality.ZERO);
		assertThat(new SemicolonOperator<JsonNode>(Collections.singletonList(new ValueLiteral<>(Jackson2JsonProviderImpl.getInstance().createNumber(1)))).getCardinality()).isEqualTo(Cardinality.ONE);
		assertThat(new SemicolonOperator<JsonNode>(Arrays.asList(new ValueLiteral<>(Jackson2JsonProviderImpl.getInstance().createNumber(1)), new BreakExpression<JsonNode>("out"))).getCardinality()).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("def f: 1; f")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testTuple() {
		assertThat(cardinalityOf("1, 2")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("1, empty")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("empty, empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("(1)")).isEqualTo(Cardinality.ONE);
	}

	@Test
	public void testBinaryOperators() {
		assertThat(cardinalityOf("1 + 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 - 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 * 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 / 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 % 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 == 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 != 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 < 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 <= 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 > 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 >= 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 and 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 or 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("1 + empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("empty + 1")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("(1, 2) + 3")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testAlternativeOperator() {
		assertThat(cardinalityOf("1 // 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("empty // 2")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("empty // empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("1 // empty")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("(1, 2) // 3")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("(empty, 1) // empty")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testConditionals() {
		assertThat(cardinalityOf("if true then 1 else 2 end")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("if true then empty else empty end")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("if (1, 2) then 1 else 2 end")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("if 1 then (1, 2) else 3 end")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("if 1 then 2 elif 3 then 4 else 5 end")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("if 1 then 2 else empty end")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testTryCatch() {
		assertThat(cardinalityOf("try empty catch empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("try 1 catch 2")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("try empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("try 1")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testAssignments() {
		assertThat(cardinalityOf(".a = 1")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".a = (1, 2)")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".a = empty")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf(".a += 1")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".a |= . + 1")).isEqualTo(Cardinality.ONE);
	}

	@Test
	public void testFieldAccess() {
		assertThat(cardinalityOf(".a")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".a?")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".[\"a\"]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".[\"a\"]?")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".[0]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".[0]?")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".[0:1]")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf(".[0:1]?")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".[]")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf(".[]?")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("empty.a")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("empty.a?")).isEqualTo(Cardinality.ZERO);
	}

	@Test
	public void testStringInterpolation() {
		assertThat(cardinalityOf("\"hello \\(.)\"")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("\"hello \\(empty)\"")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("\"hello \\((1, 2))\"")).isEqualTo(Cardinality.UNKNOWN);
	}

	@Test
	public void testReduceAndForeach() {
		assertThat(cardinalityOf("reduce .[] as $x (0; . + $x)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("reduce .[] as $x (empty; . + $x)")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("reduce .[] as $x ((1, 2); . + $x)")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("foreach .[] as $x (0; . + $x; .)")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("foreach empty as $x (0; . + $x; .)")).isEqualTo(Cardinality.ZERO);
	}

	@Test
	public void testBuiltinFunctions() {
		assertThat(cardinalityOf("length")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("type")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("not")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("keys")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("sort")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("sort_by(.)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("group_by(.)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("max_by(.)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("min_by(.)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("contains(1)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("split(\",\")")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("join(\",\")")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("delpaths([[]])")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("getpath([\"a\"])")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("setpath([\"a\"]; 1)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("error")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("error(\"msg\")")).isEqualTo(Cardinality.ZERO);
		assertThat(cardinalityOf("path(.)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("path(.a)")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("path(.a, .b)")).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("isempty(.[])")).isEqualTo(Cardinality.ONE);
		// Up to 1.6 a `try` inside the generator can make isempty emit twice; see IsEmptyFunction.
		assertThat(cardinalityOf("isempty(.[])", Versions.JQ_1_6)).isEqualTo(Cardinality.UNKNOWN);
		assertThat(cardinalityOf("@json")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@base64")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@csv")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@tsv")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@html")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@uri")).isEqualTo(Cardinality.ONE);
		assertThat(cardinalityOf("@sh")).isEqualTo(Cardinality.ONE);
	}
}
