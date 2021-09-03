package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;

public class TransformPipeComponent implements PipeComponent {
	public Expression expr;

	public TransformPipeComponent() {}

	public TransformPipeComponent(final Expression expr) {
		this.expr = expr;
	}

	public Expression getExpr() {
		return expr;
	}

	public void setExpr(Expression expr) {
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
