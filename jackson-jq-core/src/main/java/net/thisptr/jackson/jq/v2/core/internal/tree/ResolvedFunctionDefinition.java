package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.Closure;
import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.StackMemory;
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
	private final int ownClosureSlot;
	private final int definerClosureSlot;

	public ResolvedFunctionDefinition(int slot, ClosureSpec closureSpec, int fnSize, List<String> paramNames, List<Integer> paramSlots, Expression<JsonNode> resolvedBody, int ownClosureSlot, int definerClosureSlot) {
		this.slot = slot;
		this.closureSpec = closureSpec;
		this.fnSize = fnSize;
		this.paramNames = paramNames;
		this.paramSlots = paramSlots;
		this.resolvedBody = resolvedBody;
		this.ownClosureSlot = ownClosureSlot;
		this.definerClosureSlot = definerClosureSlot;
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

	public int ownClosureSlot() {
		return ownClosureSlot;
	}

	public int definerClosureSlot() {
		return definerClosureSlot;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Closure[] closureHolder = new Closure[1];
		Function factory = new Function() {
			@Override
			@SuppressWarnings("unchecked")
			public <N> Expression<N> bindArguments(JsonProvider<N> jp, List<Expression<N>> fnArgs, Version version) {
				Expression<N> effectiveBody = (Expression<N>) resolvedBody;
				return (callerFrame, input, path, out, ignoredRequirePath) -> {
					Closure effectiveClosure = (Closure) closureHolder[0];
					StackFrame fnFrame = callerFrame != null
							? callerFrame.getEnclosingMemory().pushFrame(fnSize)
							: new StackMemory().pushFrame(fnSize);
					fnFrame.set(ownClosureSlot, effectiveClosure);
					try {
						Compiler.bindAndApply(callerFrame, fnFrame, paramNames, paramSlots, fnArgs, input, path, out, (execFrame) -> {
							effectiveBody.apply(execFrame, input, path, out, false);
						});
					} finally {
						fnFrame.getEnclosingMemory().popFrame();
					}
				};
			}
		};
		if (frame != null)
			frame.set(slot, factory);
		closureHolder[0] = closureSpec.buildClosure(frame, definerClosureSlot);
	}
}
