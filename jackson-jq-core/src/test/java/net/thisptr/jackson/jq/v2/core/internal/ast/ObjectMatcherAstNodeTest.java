package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectMatcherAstNodeTest {
	@Test
	void constantKeysKeepTheirSourceForm() throws JsonQueryException {
		List<ObjectMatcherAstNode.FieldMatcher> matchers = parseObjectMatcher(". as {$x, $y: [$a], foo: $b, if: $c} | .");

		assertThat(matchers.get(0)).isInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class);
		ObjectMatcherAstNode.ConstantKeyFieldMatcher shorthand = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) matchers.get(0);
		assertThat(shorthand.dollar()).isTrue();
		assertThat(shorthand.name()).isEqualTo("x");
		assertThat(shorthand.matcher()).isNull();

		assertThat(matchers.get(1)).isInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class);
		ObjectMatcherAstNode.ConstantKeyFieldMatcher variable = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) matchers.get(1);
		assertThat(variable.dollar()).isTrue();
		assertThat(variable.name()).isEqualTo("y");
		assertThat(variable.matcher()).isInstanceOf(ArrayMatcherAstNode.class);

		assertThat(matchers.get(2)).isInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class);
		ObjectMatcherAstNode.ConstantKeyFieldMatcher identifier = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) matchers.get(2);
		assertThat(identifier.dollar()).isFalse();
		assertThat(identifier.name()).isEqualTo("foo");

		assertThat(matchers.get(3)).isInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class);
		ObjectMatcherAstNode.ConstantKeyFieldMatcher keyword = (ObjectMatcherAstNode.ConstantKeyFieldMatcher) matchers.get(3);
		assertThat(keyword.dollar()).isFalse();
		assertThat(keyword.name()).isEqualTo("if");
	}

	@Test
	void expressionKeysKeepTheirNameExpression() throws JsonQueryException {
		List<ObjectMatcherAstNode.FieldMatcher> matchers = parseObjectMatcher(". as {\"foo\": $a, \"\\(.key)\": $b, (.expr): $c} | .");

		assertThat(matchers.get(0)).isInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class);
		ObjectMatcherAstNode.ExpressionKeyFieldMatcher string = (ObjectMatcherAstNode.ExpressionKeyFieldMatcher) matchers.get(0);
		assertThat(string.name()).isInstanceOf(StringLiteralAstNode.class);

		assertThat(matchers.get(1)).isInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class);
		ObjectMatcherAstNode.ExpressionKeyFieldMatcher interpolation = (ObjectMatcherAstNode.ExpressionKeyFieldMatcher) matchers.get(1);
		assertThat(interpolation.name()).isInstanceOf(StringInterpolationAstNode.class);

		assertThat(matchers.get(2)).isInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class);
		ObjectMatcherAstNode.ExpressionKeyFieldMatcher parenthesized = (ObjectMatcherAstNode.ExpressionKeyFieldMatcher) matchers.get(2);
		assertThat(parenthesized.name()).isInstanceOf(ParenAstNode.class);
	}

	@Test
	void printedPatternsRoundTrip() throws JsonQueryException {
		assertPrintedAs(". as {$x} | $x");
		assertPrintedAs(". as {$x: [$a]} | $a");
		assertPrintedAs(". as {foo: $a} | $a");
		assertPrintedAs(". as {if: $a} | $a");
		assertPrintedAs(". as {\"foo\": $a} | $a");
		assertPrintedAs(". as {\"\\(.key)\": $a} | $a");
		assertPrintedAs(". as {(.expr): $a} | $a");
		assertPrintedAs(". as {$x, foo: {bar: $y}} | $y");
	}

	private static List<ObjectMatcherAstNode.FieldMatcher> parseObjectMatcher(String query) throws JsonQueryException {
		AstNode parsed = AstParser.parse(query, Versions.JQ_1_6);
		assertThat(parsed).isInstanceOf(BinaryOpAstNode.class);
		BinaryOpAstNode pipe = (BinaryOpAstNode) parsed;
		assertThat(pipe.operator).isEqualTo(BinaryOperator.BINDING_PIPE);
		assertThat(pipe.lhs).isInstanceOf(AsBindingAstNode.class);
		AsBindingAstNode binding = (AsBindingAstNode) pipe.lhs;
		assertThat(binding.matcher()).isInstanceOf(ObjectMatcherAstNode.class);
		return ((ObjectMatcherAstNode) binding.matcher()).matchers();
	}

	private static void assertPrintedAs(String query) throws JsonQueryException {
		AstNode parsed = AstParser.parse(query, Versions.JQ_1_6);
		assertThat(parsed.toString()).isEqualTo(query);
		assertThat(AstParser.parse(parsed.toString(), Versions.JQ_1_6).toString()).isEqualTo(query);
	}
}
