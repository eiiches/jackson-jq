package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionDefinition<JsonNode> implements Expression<JsonNode> {
	private final int slot;
	private final ClosureSpec closureSpec;
	private final int fnSize;
	private final List<String> paramNames;
	private final List<Integer> paramSlots;
	private final Expression<JsonNode> resolvedBody;

	public ResolvedFunctionDefinition(int slot, ClosureSpec closureSpec, int fnSize, List<String> paramNames, List<Integer> paramSlots, Expression<JsonNode> resolvedBody) {
		this.slot = slot;
		this.closureSpec = closureSpec;
		this.fnSize = fnSize;
		this.paramNames = paramNames;
		this.paramSlots = paramSlots;
		this.resolvedBody = resolvedBody;
	}

	public int slot() {
		return slot;
	}

	public ClosureSpec closureSpec() {
		return closureSpec;
	}

	public int fnSize() {
		return fnSize;
	}

	public List<String> paramNames() {
		return paramNames;
	}

	public List<Integer> paramSlots() {
		return paramSlots;
	}

	public Expression<JsonNode> resolvedBody() {
		return resolvedBody;
	}

	@Override
	public void apply(@Nullable StackFrame<JsonNode> frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		@SuppressWarnings("unchecked")
		Closure<JsonNode>[] closureHolder = (Closure<JsonNode>[]) new Closure<?>[1];
		FunctionFactory factory = new FunctionFactory() {
			@Override
			@SuppressWarnings("unchecked")
			public <N> Function<N> createFunction(JsonProvider<N> jp, List<Expression<N>> fnArgs, Version version) {
				Expression<N> effectiveBody = (Expression<N>) resolvedBody;
				return (callerFrame, input, path, out) -> {
					Closure<N> effectiveClosure = (Closure<N>) closureHolder[0];
					StackFrame<N> fnFrame = callerFrame != null
							? callerFrame.getStack().pushFrame(fnSize)
							: new ExecutionStack<N>().pushFrame(fnSize);
					fnFrame.setClosure(effectiveClosure);
					try {
						Compiler.bindAndApply(callerFrame, fnFrame, paramNames, paramSlots, fnArgs, input, path, out, (execFrame) -> {
							effectiveBody.apply(execFrame, input, path, out, false);
						});
					} finally {
						fnFrame.getStack().popFrame();
					}
				};
			}
		};
		if (frame != null)
			frame.set(slot, factory);
		closureHolder[0] = closureSpec.buildClosure(frame);
	}
}
