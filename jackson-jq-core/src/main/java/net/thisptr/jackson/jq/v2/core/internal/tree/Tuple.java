package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Tuple<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	public final List<Expression<StackFrame, JsonNode>> qs;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.sum(qs, Expression::getCardinality);
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public Tuple(List<Expression<StackFrame, JsonNode>> qs) {
		this.qs = qs;
		this.dependsOnInput = qs.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = qs.stream().anyMatch(Expression::dependsOnExternalState);
		this.freeLocalSlots = FreeVariables.unionAll(qs);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(qs);
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		for (Expression<StackFrame, JsonNode> q : qs) {
			q.apply(frame, in, path, output);
		}
	}
}
