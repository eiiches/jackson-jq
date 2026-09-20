package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

class PipeOperatorAstTest {
	@Test
	void pipeTokenResolvesToTheOrdinaryPipeOperator() {
		assertThat(BinaryOperator.fromString("|")).isEqualTo(BinaryOperator.PIPE);
	}

	@Test
	void pipeIsRightAssociated() throws JsonQueryException {
		BinaryOpAstNode outer = assertOperator(BinaryOperator.PIPE, AstParser.parse(".foo | .bar | .baz", Versions.JQ_1_6));
		assertThat(outer.lhs.toString()).isEqualTo(".foo");

		BinaryOpAstNode inner = assertOperator(BinaryOperator.PIPE, outer.rhs);
		assertThat(inner.lhs.toString()).isEqualTo(".bar");
		assertThat(inner.rhs.toString()).isEqualTo(".baz");
	}

	@Test
	void aBindingHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = ". as $a | . as $b | [$a, $b]";
		BinaryOpAstNode outer = assertOperator(BinaryOperator.BINDING_PIPE, AstParser.parse(query, Versions.JQ_1_6));
		assertThat(outer.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode outerBinding = (AsBindingAstNode) outer.lhs;
		assertThat(outerBinding.matcher()).isInstanceOf(ValueMatcherAstNode.class);
		assertThat(((ValueMatcherAstNode) outerBinding.matcher()).name()).isEqualTo("a");
		assertThat(outerBinding.toString()).isEqualTo(". as $a");

		BinaryOpAstNode inner = assertOperator(BinaryOperator.BINDING_PIPE, outer.rhs);
		assertThat(inner.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode innerBinding = (AsBindingAstNode) inner.lhs;
		assertThat(innerBinding.matcher()).isInstanceOf(ValueMatcherAstNode.class);
		assertThat(((ValueMatcherAstNode) innerBinding.matcher()).name()).isEqualTo("b");
		assertThat(inner.rhs.toString()).isEqualTo("[$a, $b]");

		assertThat(outer.toString()).isEqualTo(query);
	}

	@Test
	void aLabelHeadsAPipeAndDoesNotOwnWhatFollows() throws JsonQueryException {
		String query = "label $out | .foo | .bar";
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.PIPE, AstParser.parse(query, Versions.JQ_1_6));
		assertThat(pipe.lhs).isInstanceOf(LabelAstNode.class);
		LabelAstNode label = (LabelAstNode) pipe.lhs;
		assertThat(label.name()).isEqualTo("out");
		assertThat(label.toString()).isEqualTo("label $out");
		assertOperator(BinaryOperator.PIPE, pipe.rhs);

		assertThat(pipe.toString()).isEqualTo(query);
	}

	@Test
	void explicitParenthesesRemainAstBoundaries() throws JsonQueryException {
		BinaryOpAstNode leftGrouped = assertOperator(BinaryOperator.PIPE, AstParser.parse("(.foo | .bar) | .baz", Versions.JQ_1_6));
		assertThat(leftGrouped.lhs).isInstanceOf(ParenAstNode.class);

		BinaryOpAstNode rightGrouped = assertOperator(BinaryOperator.PIPE, AstParser.parse(".foo | (.bar | .baz)", Versions.JQ_1_6));
		assertThat(rightGrouped.rhs).isInstanceOf(ParenAstNode.class);
	}

	@Test
	void bindingPipeBindsMoreTightlyThanBinaryOperatorsBeforeJq18() throws JsonQueryException {
		BinaryOpAstNode plus = assertOperator(BinaryOperator.PLUS, AstParser.parse("1 + 3 as $a | $a * 2", Versions.JQ_1_7_1));
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.BINDING_PIPE, plus.rhs);
		assertThat(pipe.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode binding = (AsBindingAstNode) pipe.lhs;
		assertThat(binding.value().toString()).isEqualTo("3");
		assertOperator(BinaryOperator.TIMES, pipe.rhs);
	}

	@Test
	void versionsBeforeJq15UseTheJq15OperatorTable() throws JsonQueryException {
		BinaryOpAstNode plus = assertOperator(BinaryOperator.PLUS, AstParser.parse("1 + 3 as $a | $a * 2", Version.of(1, 4)));
		assertOperator(BinaryOperator.BINDING_PIPE, plus.rhs);
	}

	@Test
	void bindingPipeOwnsBinaryOperatorsOnTheLeftSinceJq18() throws JsonQueryException {
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.BINDING_PIPE, AstParser.parse("1 + 3 as $a | $a * 2", Versions.JQ_1_8_0));
		assertThat(pipe.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode binding = (AsBindingAstNode) pipe.lhs;
		assertOperator(BinaryOperator.PLUS, binding.value());
		assertOperator(BinaryOperator.TIMES, pipe.rhs);
	}

	@Test
	void commaRemainsOutsideTheBindingValueSinceJq18() throws JsonQueryException {
		BinaryOpAstNode comma = assertOperator(BinaryOperator.COMMA, AstParser.parse("1, 2 as $a | [$a]", Versions.JQ_1_8_0));
		BinaryOpAstNode pipe = assertOperator(BinaryOperator.BINDING_PIPE, comma.rhs);
		assertThat(pipe.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode binding = (AsBindingAstNode) pipe.lhs;
		assertThat(binding.value().toString()).isEqualTo("2");
	}

	@Test
	void bindingPipeOwnsAllOperatorsOnTheRight() throws JsonQueryException {
		for (Version version : Versions.versions()) {
			BinaryOpAstNode pipe = assertOperator(BinaryOperator.BINDING_PIPE, AstParser.parse("1 as $a | [$a], [$a + 1]", version));
			assertOperator(BinaryOperator.COMMA, pipe.rhs);
		}
	}

	private static BinaryOpAstNode assertOperator(BinaryOperator operator, AstNode node) {
		assertThat(node).isInstanceOf(BinaryOpAstNode.class);
		BinaryOpAstNode binary = (BinaryOpAstNode) node;
		assertThat(binary.operator).isEqualTo(operator);
		return binary;
	}
}
