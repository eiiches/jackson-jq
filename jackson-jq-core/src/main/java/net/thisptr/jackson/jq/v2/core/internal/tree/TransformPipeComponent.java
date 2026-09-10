package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class TransformPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression<StackFrame, JsonNode> expr;

	public TransformPipeComponent(Expression<StackFrame, JsonNode> expr) {
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
