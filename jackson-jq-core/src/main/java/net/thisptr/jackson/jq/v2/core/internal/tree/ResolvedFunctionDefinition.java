package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.AstResolver;
import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionDefinition implements Expression {
	private final int slot;
	private final ClosureSpec closureSpec;
	private final int fnSize;
	private final List<String> paramNames;
	private final List<Integer> paramSlots;
	private final Expression resolvedBody;

	public ResolvedFunctionDefinition(int slot, ClosureSpec closureSpec, int fnSize, List<String> paramNames, List<Integer> paramSlots, Expression resolvedBody) {
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

	public Expression resolvedBody() {
		return resolvedBody;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		@SuppressWarnings("unchecked")
		Closure<JsonNode>[] closureHolder = new Closure[1];
		FunctionFactory factory = new FunctionFactory() {
			@Override
			@SuppressWarnings({"unchecked", "rawtypes"})
			public <N> Function<N> createFunction(net.thisptr.jackson.jq.v2.json.JsonProvider<N> jsonProvider, List<Expression> fnArgs, Version version) {
				return (runtimeScope, input, path, out) -> {
					Closure<N> effectiveClosure = (Closure<N>) closureHolder[0];
					ExecutionStack<N>.Frame fnFrame = runtimeScope.getExecutionFrame() != null
							? runtimeScope.getExecutionFrame().getStack().pushFrame(runtimeScope.getExecutionFrame(), fnSize)
							: new ExecutionStack<N>().pushFrame(null, fnSize);
					fnFrame.setClosure(effectiveClosure);
					Scope<N> fnScope = Scope.newChildScopeWithFrame(runtimeScope, fnFrame);
					try {
						AstResolver.bindAndApply(runtimeScope, fnScope, paramNames, paramSlots, fnArgs, input, path, out, (execScope) -> {
							resolvedBody.apply(execScope, input, path, out, false);
						});
					} finally {
						fnFrame.getStack().popFrame();
					}
				};
			}
		};
		scope.setFunctionFactory(slot, factory);
		closureHolder[0] = closureSpec.buildClosure(scope.getExecutionFrame());
	}
}
