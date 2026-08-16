package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootExpression<JsonNode> implements Expression<JsonNode> {
	private final int frameSize;
	private final Expression<JsonNode> inner;

	public RootExpression(int frameSize, Expression<JsonNode> inner) {
		this.frameSize = frameSize;
		this.inner = inner;
	}

	public int frameSize() {
		return frameSize;
	}

	public Expression<JsonNode> inner() {
		return inner;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		ExecutionStack<JsonNode>.Frame rootFrame = parentFrame != null
				? parentFrame.getStack().pushFrame(parentFrame, frameSize)
				: new ExecutionStack<JsonNode>().pushFrame(null, frameSize);
		try {
			inner.apply(rootFrame, in, path, output, requirePath);
		} finally {
			rootFrame.getStack().popFrame();
		}
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
