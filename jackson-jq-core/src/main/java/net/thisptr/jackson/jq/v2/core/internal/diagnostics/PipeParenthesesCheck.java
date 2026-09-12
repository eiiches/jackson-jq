package net.thisptr.jackson.jq.v2.core.internal.diagnostics;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.core.internal.ast.AsBindingAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipeAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;

/**
 * Warns about a {@code ,} written as an operand of a {@code |} without parentheses, as in
 * {@code a, b | .}.
 * <p>
 * {@code ,} binds tighter than {@code |}, so {@code a, b | .} means {@code (a, b) | .} -- but it
 * reads just as easily as {@code a, (b | .)}, which is what jq does <em>not</em> do. Parentheses
 * settle it either way. jq itself does not warn about this; the check exists only for callers who
 * ask for diagnostics.
 */
public final class PipeParenthesesCheck extends AbstractAstWalker {
	private final DiagnosticListener listener;

	private PipeParenthesesCheck(DiagnosticListener listener) {
		this.listener = listener;
	}

	/**
	 * Reports every unparenthesised {@code ,} operand of a {@code |} in {@code ast}, in source order.
	 *
	 * @param ast the parsed query
	 * @param listener receives the warnings
	 */
	public static void run(AstNode ast, DiagnosticListener listener) {
		new PipeParenthesesCheck(listener).walk(ast);
	}

	// Each side is reported before being descended into, so the warnings come out in source order.
	@Override
	public Void visit(PipeAstNode node) {
		check(node.left());
		walk(node.left());
		check(node.right());
		walk(node.right());
		return null;
	}

	// The value an `as` binding matches is the one operand a pipe head has of its own, and it sits
	// left of a `|` just as ambiguously as any other left operand. What follows the `|` is the
	// pipe's right side, checked there.
	@Override
	public Void visit(AsBindingAstNode node) {
		check(node.value());
		return super.visit(node);
	}

	private void check(AstNode operand) {
		if (!(operand instanceof TupleAstNode))
			return;
		listener.report(Diagnostic.warning(
				"`,` binds tighter than `|`: write `(" + operand + ")` to make the grouping explicit",
				operand.location()));
	}
}
