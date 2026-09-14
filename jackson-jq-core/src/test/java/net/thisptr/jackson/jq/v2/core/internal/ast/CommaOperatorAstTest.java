package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CommaOperatorAstTest {
	@Test
	void commaIsLeftAssociated() throws JsonQueryException {
		String query = ".foo, .bar, .baz";
		BinaryOpAstNode outer = assertOperator(BinaryOperator.COMMA, AstParser.parse(query, Versions.JQ_1_6));
		assertEquals(".baz", outer.rhs.toString());

		BinaryOpAstNode inner = assertOperator(BinaryOperator.COMMA, outer.lhs);
		assertEquals(".foo", inner.lhs.toString());
		assertEquals(".bar", inner.rhs.toString());

		assertEquals(query, outer.toString());
	}

	@Test
	void aSingleOperandIsNotWrappedInAComma() throws JsonQueryException {
		assertInstanceOf(IdentifierFieldAccessAstNode.class, AstParser.parse(".foo", Versions.JQ_1_6));
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		BinaryOpAstNode rightGrouped = assertOperator(BinaryOperator.COMMA, AstParser.parse(".foo, (.bar, .baz)", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, rightGrouped.rhs);

		BinaryOpAstNode leftGrouped = assertOperator(BinaryOperator.COMMA, AstParser.parse("(.foo, .bar), .baz", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, leftGrouped.lhs);
	}

	// `,` binds tighter than `|` and looser than every binary operator, so it owns neither side here.
	@Test
	void commaSitsBetweenThePipeAndTheBinaryOperators() throws JsonQueryException {
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.PIPE, AstParser.parse("1, 2 | .", Versions.JQ_1_6));
		assertOperator(BinaryOperator.COMMA, pipe.lhs);

		BinaryOpAstNode comma = assertOperator(BinaryOperator.COMMA, AstParser.parse("1 + 2, 3", Versions.JQ_1_6));
		assertOperator(BinaryOperator.PLUS, comma.lhs);
	}

	private static BinaryOpAstNode assertOperator(BinaryOperator operator, AstNode node) {
		BinaryOpAstNode binary = assertInstanceOf(BinaryOpAstNode.class, node);
		assertEquals(operator, binary.operator);
		return binary;
	}
}
