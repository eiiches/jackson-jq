package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PipeAstNodeTest {
	@Test
	void pipeIsRightAssociated() throws JsonQueryException {
		PipeAstNode outer = assertInstanceOf(PipeAstNode.class, AstParser.parse(".foo | .bar | .baz", Versions.JQ_1_6));
		assertEquals(".foo", outer.left().toString());

		PipeAstNode inner = assertInstanceOf(PipeAstNode.class, outer.right());
		assertEquals(".bar", inner.left().toString());
		assertEquals(".baz", inner.right().toString());
	}

	@Test
	void aBindingHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = ". as $a | . as $b | [$a, $b]";
		PipeAstNode outer = assertInstanceOf(PipeAstNode.class, AstParser.parse(query, Versions.JQ_1_6));
		AsBindingAstNode outerBinding = assertInstanceOf(AsBindingAstNode.class, outer.left());
		assertEquals("a", assertInstanceOf(ValueMatcherAstNode.class, outerBinding.matcher()).name());
		assertEquals(". as $a", outerBinding.toString());

		PipeAstNode inner = assertInstanceOf(PipeAstNode.class, outer.right());
		AsBindingAstNode innerBinding = assertInstanceOf(AsBindingAstNode.class, inner.left());
		assertEquals("b", assertInstanceOf(ValueMatcherAstNode.class, innerBinding.matcher()).name());
		assertEquals("[$a, $b]", inner.right().toString());

		assertEquals(query, outer.toString());
	}

	@Test
	void aLabelHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = "label $out | .foo | .bar";
		PipeAstNode pipe = assertInstanceOf(PipeAstNode.class, AstParser.parse(query, Versions.JQ_1_6));
		LabelAstNode label = assertInstanceOf(LabelAstNode.class, pipe.left());
		assertEquals("out", label.name());
		assertEquals("label $out", label.toString());
		assertInstanceOf(PipeAstNode.class, pipe.right());

		assertEquals(query, pipe.toString());
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		PipeAstNode leftGrouped = assertInstanceOf(PipeAstNode.class, AstParser.parse("(.foo | .bar) | .baz", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, leftGrouped.left());

		PipeAstNode rightGrouped = assertInstanceOf(PipeAstNode.class, AstParser.parse(".foo | (.bar | .baz)", Versions.JQ_1_6));
		assertInstanceOf(ParenAstNode.class, rightGrouped.right());
	}
}
