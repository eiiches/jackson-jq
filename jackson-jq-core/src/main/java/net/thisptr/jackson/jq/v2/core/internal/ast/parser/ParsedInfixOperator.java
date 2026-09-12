package net.thisptr.jackson.jq.v2.core.internal.ast.parser;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PatternMatcherAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

/**
 * A parsed infix operator, including syntax that affects how an operator is reduced.
 */
public final class ParsedInfixOperator {
	private final BinaryOperator operator;
	private final @Nullable PatternMatcherAstNode matcher;

	private ParsedInfixOperator(BinaryOperator operator, @Nullable PatternMatcherAstNode matcher) {
		this.operator = operator;
		this.matcher = matcher;
	}

	public static ParsedInfixOperator of(BinaryOperator operator) {
		if (operator == BinaryOperator.BINDING_PIPE)
			throw new IllegalArgumentException("BINDING_PIPE requires a matcher");
		return new ParsedInfixOperator(operator, null);
	}

	/**
	 * Returns a pipe whose left operand is followed by an {@code as} matcher.
	 */
	public static ParsedInfixOperator bindingPipe(PatternMatcherAstNode matcher) {
		return new ParsedInfixOperator(BinaryOperator.BINDING_PIPE, matcher);
	}

	BinaryOperator operator() {
		return operator;
	}

	AstNode createNode(AstNode lhs, AstNode rhs) {
		if (!isPipeOperator(operator) && (isPipeHead(lhs) || isPipeHead(rhs)))
			throw new IllegalStateException("Assignment or label must be followed by pipes: " + (isPipeHead(lhs) ? lhs : rhs));
		if (isPipeOperator(operator) && isPipeHead(rhs))
			throw new IllegalStateException("Pipe cannot be terminated by assignment or label: | " + rhs);

		@Var AstNode left = lhs;
		if (matcher != null) {
			if (isPipeHead(lhs))
				throw new IllegalStateException("Assignment or label must be followed by pipes: " + lhs);
			left = new AsBindingAstNode(SourceLocation.span(lhs.location(), matcher.location()), lhs, matcher);
		}
		return new BinaryOpAstNode(SourceLocation.span(left.location(), rhs.location()), operator, left, rhs);
	}

	static void checkCompleteExpression(AstNode expression) {
		if (isPipeHead(expression))
			throw new IllegalStateException("Assignment or label must be followed by pipes: " + expression);
	}

	private static boolean isPipeHead(AstNode operand) {
		return operand instanceof AsBindingAstNode || operand instanceof LabelAstNode;
	}

	private static boolean isPipeOperator(BinaryOperator operator) {
		return operator == BinaryOperator.PIPE || operator == BinaryOperator.BINDING_PIPE;
	}

	@Override
	public String toString() {
		return operator.toString();
	}
}
