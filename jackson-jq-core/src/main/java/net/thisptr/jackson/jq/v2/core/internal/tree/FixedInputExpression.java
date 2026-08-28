package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Restores the enclosing query's fixed-input dependency view after compiling a call locally.
 */
public final class FixedInputExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> delegate;

	public FixedInputExpression(Expression<StackFrame, JsonNode> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Cardinality getCardinality() {
		return delegate.getCardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return false;
	}

	@Override
	public boolean dependsOnExternalState() {
		return delegate.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.slotsOf(delegate);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.opaqueIn(delegate);
	}

	@Override
	public void apply(StackFrame context, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		delegate.apply(context, in, ipath, output);
	}
}
