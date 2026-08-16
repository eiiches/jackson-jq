package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootExpression<JsonNode> implements Expression {
	private final int frameSize;
	private final Expression inner;

	public RootExpression(int frameSize, Expression inner) {
		this.frameSize = frameSize;
		this.inner = inner;
	}

	public int frameSize() {
		return frameSize;
	}

	public Expression inner() {
		return inner;
	}

	@Override
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> path, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		ExecutionStack<N>.Frame parentFrame = scope.getExecutionFrame();
		ExecutionStack<N>.Frame rootFrame = parentFrame != null
				? parentFrame.getStack().pushFrame(parentFrame, frameSize)
				: new ExecutionStack<N>().pushFrame(null, frameSize);
		Scope<N> rootExecScope = Scope.newChildScopeWithFrame(scope, rootFrame);
		try {
			inner.apply(rootExecScope, in, path, output, requirePath);
		} finally {
			rootFrame.getStack().popFrame();
		}
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
