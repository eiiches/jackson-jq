package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A run of {@code ,}-separated expressions, evaluated left to right into one output stream.
 * <p>
 * {@code ,} is a binary node in the AST, but a chain of them is flattened here rather than mirrored:
 * the nesting carries no meaning -- every operand sees the same input and the same path, and their
 * outputs are simply concatenated -- so evaluating {@code a, b, c, ...} is one loop instead of a
 * stack frame per comma.
 */
public class Comma<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final List<Expression<StackFrame, JsonNode>> operands;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public Comma(List<Expression<StackFrame, JsonNode>> operands) {
		this.operands = operands;
		this.dependsOnInput = operands.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = operands.stream().anyMatch(Expression::dependsOnExternalState);
		this.freeLocalSlots = FreeVariables.unionAll(operands);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(operands);
	}

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.sum(operands, Expression::getCardinality);
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

	// Every operand gets the caller's path unchanged -- a comma does not re-root `.` the way a pipe
	// does -- so there is none of PipedQuery's path shielding here.
	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		for (Expression<StackFrame, JsonNode> operand : operands) {
			operand.apply(frame, in, path, output);
		}
	}
}
