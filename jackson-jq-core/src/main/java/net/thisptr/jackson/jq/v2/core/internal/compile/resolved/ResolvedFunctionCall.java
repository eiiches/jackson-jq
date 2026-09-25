package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * An immutable executable static call. Captured arguments cannot be rewritten without rebinding.
 */
public final class ResolvedFunctionCall<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> function;
	private final ExpressionProperties properties;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ResolvedFunctionCall(Expression<StackFrame, JsonNode> function, ExpressionProperties properties,
								List<? extends Expression<StackFrame, JsonNode>> arguments) {
		this.function = function;
		this.properties = properties;
		this.freeLocalSlots = FreeVariables.unionAll(arguments);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(arguments);
	}

	@Override
	public net.thisptr.jackson.jq.v2.spi.Cardinality getCardinality() {
		return properties.cardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return properties.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return properties.dependsOnExternalState();
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
		function.apply(frame, in, path, output);
	}
}
