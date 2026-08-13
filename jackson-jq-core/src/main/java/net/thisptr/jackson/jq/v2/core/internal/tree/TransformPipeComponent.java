package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.spi.Expression;

public class TransformPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression expr;

	public TransformPipeComponent(Expression expr) {
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
