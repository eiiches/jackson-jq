package net.thisptr.jackson.jq.v2.core.internal.diagnostics;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

/**
 * Warns where a pipe and an ordinary operator are written next to each other without parentheses
 * and the resulting grouping is not the one the source reads like.
 * <p>
 * {@code ,} binds tighter than {@code |}, so {@code a, b | .} means {@code (a, b) | .} -- but it
 * reads just as easily as {@code a, (b | .)}, which is what jq does <em>not</em> do.
 * <p>
 * The pipe an {@code as} introduces is the mirror image: it takes only the value immediately left
 * of the {@code as} and then owns everything to its right, so it can end up buried as the right
 * operand of an operator that appears to enclose it. {@code a, b as $x | .} means
 * {@code a, (b as $x | .)} however much it reads like {@code (a, b) as $x | .}, and before jq 1.8
 * -- where a binding pipe outranks every ordinary operator rather than tying with {@code ,} --
 * {@code a + b as $x | .} likewise means {@code a + (b as $x | .)}. Which of those the parser
 * builds is therefore version-dependent, but this check needs no version branch of its own: it
 * reads an AST that already reflects the version in force.
 * <p>
 * A binding pipe owns everything to its right, so it can never be an operator's <em>left</em>
 * operand; only right operands are examined. Parentheses settle every case. jq itself does not
 * warn about any of this; the check exists only for callers who ask for diagnostics.
 */
public final class PipeParenthesesCheck extends AbstractAstWalker {
	private final DiagnosticListener listener;

	private PipeParenthesesCheck(DiagnosticListener listener) {
		this.listener = listener;
	}

	/**
	 * Reports every unparenthesised grouping of a pipe with an ordinary operator in {@code ast},
	 * in source order.
	 *
	 * @param ast the parsed query
	 * @param listener receives the warnings
	 */
	public static void run(AstNode ast, DiagnosticListener listener) {
		new PipeParenthesesCheck(listener).walk(ast);
	}

	// Each side is reported before being descended into, so the warnings come out in source order.
	@Override
	public Void visit(BinaryOpAstNode node) {
		// An ordinary `|` always swallows the operator to its left, so a binding pipe is the only pipe
		// that can turn up underneath one.
		if (node.operator != BinaryOperator.PIPE && node.operator != BinaryOperator.BINDING_PIPE) {
			checkBindingPipe(node.rhs);
			return super.visit(node);
		}
		checkComma(node.lhs);
		walk(node.lhs);
		checkComma(node.rhs);
		walk(node.rhs);
		return null;
	}

	// The value an `as` binding matches is the one operand a pipe head has of its own, and it sits
	// left of a `|` just as ambiguously as any other left operand. What follows the `|` is the
	// pipe's right side, checked there.
	@Override
	public Void visit(AsBindingAstNode node) {
		checkComma(node.value());
		return super.visit(node);
	}

	private void checkComma(AstNode operand) {
		if (!isOperator(operand, BinaryOperator.COMMA))
			return;
		listener.report(Diagnostic.warning(
				"`,` binds tighter than `|`: write `(" + operand + ")` to make the grouping explicit",
				operand.location()));
	}

	// How much of what precedes the `as` gets bound is exactly the version-dependent part, so the
	// message names the bound value rather than describing it.
	private void checkBindingPipe(AstNode operand) {
		if (!isOperator(operand, BinaryOperator.BINDING_PIPE))
			return;
		AstNode bound = ((AsBindingAstNode) ((BinaryOpAstNode) operand).lhs).value();
		listener.report(Diagnostic.warning(
				"`as` binds only `" + bound + "`: write `(" + operand + ")` to make the grouping explicit",
				operand.location()));
	}

	private static boolean isOperator(AstNode operand, BinaryOperator operator) {
		return operand instanceof BinaryOpAstNode && ((BinaryOpAstNode) operand).operator == operator;
	}
}
