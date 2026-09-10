package net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.ParenAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.PipedQueryAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.StringInterpolationAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.StringLiteralAstNode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMatcherAstNodeTest {
	@Test
	void constantKeysKeepTheirSourceForm() throws JsonQueryException {
		List<ObjectMatcherAstNode.FieldMatcher> matchers = parseObjectMatcher(". as {$x, $y: [$a], foo: $b, if: $c} | .");

		ObjectMatcherAstNode.ConstantKeyFieldMatcher shorthand = assertInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class, matchers.get(0));
		assertTrue(shorthand.dollar());
		assertEquals("x", shorthand.name());
		assertNull(shorthand.matcher());

		ObjectMatcherAstNode.ConstantKeyFieldMatcher variable = assertInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class, matchers.get(1));
		assertTrue(variable.dollar());
		assertEquals("y", variable.name());
		assertInstanceOf(ArrayMatcherAstNode.class, variable.matcher());

		ObjectMatcherAstNode.ConstantKeyFieldMatcher identifier = assertInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class, matchers.get(2));
		assertFalse(identifier.dollar());
		assertEquals("foo", identifier.name());

		ObjectMatcherAstNode.ConstantKeyFieldMatcher keyword = assertInstanceOf(ObjectMatcherAstNode.ConstantKeyFieldMatcher.class, matchers.get(3));
		assertFalse(keyword.dollar());
		assertEquals("if", keyword.name());
	}

	@Test
	void expressionKeysKeepTheirNameExpression() throws JsonQueryException {
		List<ObjectMatcherAstNode.FieldMatcher> matchers = parseObjectMatcher(". as {\"foo\": $a, \"\\(.key)\": $b, (.expr): $c} | .");

		ObjectMatcherAstNode.ExpressionKeyFieldMatcher string = assertInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class, matchers.get(0));
		assertInstanceOf(StringLiteralAstNode.class, string.name());

		ObjectMatcherAstNode.ExpressionKeyFieldMatcher interpolation = assertInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class, matchers.get(1));
		assertInstanceOf(StringInterpolationAstNode.class, interpolation.name());

		ObjectMatcherAstNode.ExpressionKeyFieldMatcher parenthesized = assertInstanceOf(ObjectMatcherAstNode.ExpressionKeyFieldMatcher.class, matchers.get(2));
		assertInstanceOf(ParenAstNode.class, parenthesized.name());
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
		PipedQueryAstNode piped = assertInstanceOf(PipedQueryAstNode.class, AstParser.parse(query, Versions.JQ_1_6));
		PipedQueryAstNode.AssignPipeComponent assign = assertInstanceOf(PipedQueryAstNode.AssignPipeComponent.class, piped.components().get(0));
		return assertInstanceOf(ObjectMatcherAstNode.class, assign.matcher).matchers();
	}

	private static void assertPrintedAs(String query) throws JsonQueryException {
		AstNode parsed = AstParser.parse(query, Versions.JQ_1_6);
		assertEquals(query, parsed.toString());
		assertEquals(query, AstParser.parse(parsed.toString(), Versions.JQ_1_6).toString());
	}
}
