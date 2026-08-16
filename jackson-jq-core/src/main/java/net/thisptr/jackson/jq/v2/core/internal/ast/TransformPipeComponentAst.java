package net.thisptr.jackson.jq.v2.core.internal.ast;

public class TransformPipeComponentAst implements PipeComponentAst {
	public final AstNode expr;

	public TransformPipeComponentAst(AstNode expr) {
		this.expr = expr;
	}

	@Override
	public boolean canTerminatePipe() {
		return true;
	}

	@Override
	public String toString() {
		return expr.toString();
	}
}
