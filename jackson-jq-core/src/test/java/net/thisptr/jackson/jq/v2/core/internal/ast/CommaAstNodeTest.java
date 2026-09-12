package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CommaAstNodeTest {
	@Test
	void commaIsLeftAssociated() throws JsonQueryException {
		String query = ".foo, .bar, .baz";
		CommaAstNode outer = assertInstanceOf(CommaAstNode.class, AstParser.parse(query, Versions.JQ_1_6));
		assertEquals(".baz", outer.right().toString());

		CommaAstNode inner = assertInstanceOf(CommaAstNode.class, outer.left());
		assertEquals(".foo", inner.left().toString());
		assertEquals(".bar", inner.right().toString());

		assertEquals(query, outer.toString());
	}

	@Test
	void aSingleOperandIsNotWrappedInAComma() throws JsonQueryException {
		assertInstanceOf(IdentifierFieldAccessAstNode.class, AstParser.parse(".foo", Versions.JQ_1_6));
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		CommaAstNode rightGrouped = assertInstanceOf(CommaAstNode.class, AstParser.parse(".foo, (.bar, .baz)", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, rightGrouped.right());

		CommaAstNode leftGrouped = assertInstanceOf(CommaAstNode.class, AstParser.parse("(.foo, .bar), .baz", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, leftGrouped.left());
	}

	// `,` binds tighter than `|` and looser than every binary operator, so it owns neither side here.
	@Test
	void commaSitsBetweenThePipeAndTheBinaryOperators() throws JsonQueryException {
		PipeAstNode pipe = assertInstanceOf(PipeAstNode.class, AstParser.parse("1, 2 | .", Versions.JQ_1_6));
		assertInstanceOf(CommaAstNode.class, pipe.left());

		CommaAstNode comma = assertInstanceOf(CommaAstNode.class, AstParser.parse("1 + 2, 3", Versions.JQ_1_6));
		assertInstanceOf(BinaryOpAstNode.class, comma.left());
	}
}
