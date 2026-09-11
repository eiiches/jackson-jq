package net.thisptr.jackson.jq.v2.core.internal.diagnostics;

import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.DiagnosticListener;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.LabelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipedQueryAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TupleAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.VariableBindingAstNode;

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

	@Override
	public Void visit(PipedQueryAstNode node) {
		check(node.left());
		check(node.right());
		return super.visit(node);
	}

	// `f as $x | body` and `label $out | body` are the pipe's other two shapes: both are written
	// with a `|`, so a bare comma on either side of it reads exactly as ambiguously.
	@Override
	public Void visit(VariableBindingAstNode node) {
		check(node.value());
		check(node.body());
		return super.visit(node);
	}

	@Override
	public Void visit(LabelAstNode node) {
		check(node.body());
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
