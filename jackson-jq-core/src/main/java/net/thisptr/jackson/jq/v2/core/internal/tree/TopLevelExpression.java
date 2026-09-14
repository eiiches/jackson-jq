package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TopLevelExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> expr;

	@Override
	public Cardinality getCardinality() {
		return expr.getCardinality();
	}

	public TopLevelExpression(Expression<StackFrame, JsonNode> expr) {
		this.expr = expr;
	}

	@Override
	public boolean dependsOnInput() {
		return expr.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return expr.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(expr);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(expr);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		expr.apply(frame, in, ipath, output);
	}
}
