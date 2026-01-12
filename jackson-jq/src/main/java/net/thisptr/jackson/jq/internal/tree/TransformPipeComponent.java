package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;

public class TransformPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression<JsonNode> expr;

	public TransformPipeComponent(final Expression<JsonNode> expr) {
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
