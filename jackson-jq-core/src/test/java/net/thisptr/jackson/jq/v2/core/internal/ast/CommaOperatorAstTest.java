package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

class CommaOperatorAstTest {
	@Test
	void commaIsLeftAssociated() throws JsonQueryException {
		String query = ".foo, .bar, .baz";
		BinaryOpAstNode outer = assertOperator(BinaryOperator.COMMA, AstParser.parse(query, Versions.JQ_1_6));
		assertThat(outer.rhs.toString()).isEqualTo(".baz");

		BinaryOpAstNode inner = assertOperator(BinaryOperator.COMMA, outer.lhs);
		assertThat(inner.lhs.toString()).isEqualTo(".foo");
		assertThat(inner.rhs.toString()).isEqualTo(".bar");

		assertThat(outer.toString()).isEqualTo(query);
	}

	@Test
	void aSingleOperandIsNotWrappedInAComma() throws JsonQueryException {
		assertThat(AstParser.parse(".foo", Versions.JQ_1_6)).isInstanceOf(IdentifierFieldAccessAstNode.class);
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		BinaryOpAstNode rightGrouped = assertOperator(BinaryOperator.COMMA, AstParser.parse(".foo, (.bar, .baz)", Versions.JQ_1_6));
		assertThat(rightGrouped.rhs).isInstanceOf(ParenAstNode.class);

		BinaryOpAstNode leftGrouped = assertOperator(BinaryOperator.COMMA, AstParser.parse("(.foo, .bar), .baz", Versions.JQ_1_6));
		assertThat(leftGrouped.lhs).isInstanceOf(ParenAstNode.class);
	}

	// `,` binds tighter than `|` and looser than every binary operator, so it owns neither side here.
	@Test
	void commaSitsBetweenThePipeAndTheBinaryOperators() throws JsonQueryException {
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.PIPE, AstParser.parse("1, 2 | .", Versions.JQ_1_6));
		assertOperator(BinaryOperator.COMMA, pipe.lhs);

		BinaryOpAstNode comma = assertOperator(BinaryOperator.COMMA, AstParser.parse("1 + 2, 3", Versions.JQ_1_6));
		assertOperator(BinaryOperator.PLUS, comma.lhs);
	}

	@Test
	void definitionAfterCommaScopesItsContinuation() throws JsonQueryException {
		BinaryOpAstNode comma = assertOperator(BinaryOperator.COMMA, AstParser.parse("1, def f: 2; f | 4", Versions.JQ_1_6));
		assertThat(comma.lhs.toString()).isEqualTo("1");
		assertThat(comma.rhs).isInstanceOf(SemicolonOperatorAstNode.class);
		SemicolonOperatorAstNode definition = (SemicolonOperatorAstNode) comma.rhs;
		assertThat(definition.expressions()).hasSize(2);
		assertOperator(BinaryOperator.PIPE, definition.expressions().get(1));

		BinaryOpAstNode groupedPipe = assertOperator(BinaryOperator.PIPE, AstParser.parse("1, (def f: 2; f) | 4", Versions.JQ_1_6));
		BinaryOpAstNode groupedComma = assertOperator(BinaryOperator.COMMA, groupedPipe.lhs);
		assertThat(groupedComma.rhs).isInstanceOf(ParenAstNode.class);
	}

	private static BinaryOpAstNode assertOperator(BinaryOperator operator, AstNode node) {
		assertThat(node).isInstanceOf(BinaryOpAstNode.class);
		BinaryOpAstNode binary = (BinaryOpAstNode) node;
		assertThat(binary.operator).isEqualTo(operator);
		return binary;
	}
}
