package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class SemicolonOperator<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private List<Expression<StackFrame, JsonNode>> qs;

	@Override
	public Cardinality getCardinality() {
		return qs.isEmpty() ? Cardinality.ZERO : qs.get(qs.size() - 1).getCardinality();
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public SemicolonOperator(List<Expression<StackFrame, JsonNode>> qs) {
		this.qs = qs;
		this.dependsOnInput = qs.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = qs.stream().anyMatch(Expression::dependsOnExternalState);
		this.freeLocalSlots = FreeVariables.unionAll(qs);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(qs);
	}

	public List<Expression<StackFrame, JsonNode>> expressions() {
		return qs;
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		if (qs.isEmpty())
			return;
		for (Expression<StackFrame, JsonNode> q : qs.subList(0, qs.size() - 1))
			q.apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> {
			});
		qs.get(qs.size() - 1).apply(frame, in, path, output);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		@Var String sep = "";
		for (Expression<StackFrame, JsonNode> q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
