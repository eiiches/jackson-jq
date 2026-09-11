package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PipedQueryAstNodeTest {
	@Test
	void pipeIsRightAssociated() throws JsonQueryException {
		PipedQueryAstNode outer = assertInstanceOf(PipedQueryAstNode.class, AstParser.parse(".foo | .bar | .baz", Versions.JQ_1_6));
		assertEquals(".foo", outer.left().toString());

		PipedQueryAstNode inner = assertInstanceOf(PipedQueryAstNode.class, outer.right());
		assertEquals(".bar", inner.left().toString());
		assertEquals(".baz", inner.right().toString());
	}

	@Test
	void bindingOwnsTheRemainingExpression() throws JsonQueryException {
		VariableBindingAstNode outer = assertInstanceOf(VariableBindingAstNode.class, AstParser.parse(". as $a | . as $b | [$a, $b]", Versions.JQ_1_6));
		assertEquals("a", assertInstanceOf(ValueMatcherAstNode.class, outer.matcher()).name());

		VariableBindingAstNode inner = assertInstanceOf(VariableBindingAstNode.class, outer.body());
		assertEquals("b", assertInstanceOf(ValueMatcherAstNode.class, inner.matcher()).name());
		assertEquals("[$a, $b]", inner.body().toString());
	}

	@Test
	void labelOwnsTheRemainingExpression() throws JsonQueryException {
		LabelAstNode label = assertInstanceOf(LabelAstNode.class, AstParser.parse("label $out | .foo | .bar", Versions.JQ_1_6));
		assertEquals("out", label.name());
		assertInstanceOf(PipedQueryAstNode.class, label.body());
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		PipedQueryAstNode leftGrouped = assertInstanceOf(PipedQueryAstNode.class, AstParser.parse("(.foo | .bar) | .baz", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, leftGrouped.left());

		PipedQueryAstNode rightGrouped = assertInstanceOf(PipedQueryAstNode.class, AstParser.parse(".foo | (.bar | .baz)", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, rightGrouped.right());
	}
}
