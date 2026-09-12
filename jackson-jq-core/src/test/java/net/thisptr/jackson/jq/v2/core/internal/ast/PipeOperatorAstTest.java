package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PipeOperatorAstTest {
	@Test
	void pipeTokenResolvesToTheOrdinaryPipeOperator() {
		assertEquals(BinaryOperator.PIPE, BinaryOperator.fromString("|"));
	}

	@Test
	void pipeIsRightAssociated() throws JsonQueryException {
		BinaryOpAstNode outer = assertOperator(BinaryOperator.PIPE, AstParser.parse(".foo | .bar | .baz", Versions.JQ_1_6));
		assertEquals(".foo", outer.lhs.toString());

		BinaryOpAstNode inner = assertOperator(BinaryOperator.PIPE, outer.rhs);
		assertEquals(".bar", inner.lhs.toString());
		assertEquals(".baz", inner.rhs.toString());
	}

	@Test
	void aBindingHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = ". as $a | . as $b | [$a, $b]";
		BinaryOpAstNode outer = assertOperator(BinaryOperator.BINDING_PIPE, AstParser.parse(query, Versions.JQ_1_6));
		AsBindingAstNode outerBinding = assertInstanceOf(AsBindingAstNode.class, outer.lhs);
		assertEquals("a", assertInstanceOf(ValueMatcherAstNode.class, outerBinding.matcher()).name());
		assertEquals(". as $a", outerBinding.toString());

		BinaryOpAstNode inner = assertOperator(BinaryOperator.BINDING_PIPE, outer.rhs);
		AsBindingAstNode innerBinding = assertInstanceOf(AsBindingAstNode.class, inner.lhs);
		assertEquals("b", assertInstanceOf(ValueMatcherAstNode.class, innerBinding.matcher()).name());
		assertEquals("[$a, $b]", inner.rhs.toString());

		assertEquals(query, outer.toString());
	}

	@Test
	void aLabelHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = "label $out | .foo | .bar";
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.PIPE, AstParser.parse(query, Versions.JQ_1_6));
		LabelAstNode label = assertInstanceOf(LabelAstNode.class, pipe.lhs);
		assertEquals("out", label.name());
		assertEquals("label $out", label.toString());
		assertOperator(BinaryOperator.PIPE, pipe.rhs);

		assertEquals(query, pipe.toString());
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		BinaryOpAstNode leftGrouped = assertOperator(BinaryOperator.PIPE, AstParser.parse("(.foo | .bar) | .baz", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, leftGrouped.lhs);

		BinaryOpAstNode rightGrouped = assertOperator(BinaryOperator.PIPE, AstParser.parse(".foo | (.bar | .baz)", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, rightGrouped.rhs);
	}

	@Test
	void bindingPipeOwnsTheFullyReducedLeftOperand() throws JsonQueryException {
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.BINDING_PIPE, AstParser.parse("1 + 3 as $a | $a * 2", Versions.JQ_1_6));
		AsBindingAstNode binding = assertInstanceOf(AsBindingAstNode.class, pipe.lhs);
		assertOperator(BinaryOperator.PLUS, binding.value());
		assertOperator(BinaryOperator.TIMES, pipe.rhs);
	}

	private static BinaryOpAstNode assertOperator(BinaryOperator operator, AstNode node) {
		BinaryOpAstNode binary = assertInstanceOf(BinaryOpAstNode.class, node);
		assertEquals(operator, binary.operator);
		return binary;
	}
}
