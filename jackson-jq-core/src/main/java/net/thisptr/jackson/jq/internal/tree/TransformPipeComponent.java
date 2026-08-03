package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;

public class TransformPipeComponent implements PipeComponent {
	public final Expression expr;

	public TransformPipeComponent(final Expression expr) {
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
