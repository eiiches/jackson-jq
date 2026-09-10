package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Expression;

public abstract class AbstractBinaryOperatorExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	protected final Expression<StackFrame, JsonNode> lhs;
	protected final Expression<StackFrame, JsonNode> rhs;
	private final String image;
	// Default `lhs || rhs` formulas shared by every non-assignment operator (arithmetic, comparison,
	// and/or, //). The assignment family (whose dependsOnInput additionally depends on whether `.`
	// itself is known fixed -- see Assignment/AbstractComplexAssignment/UpdateAssignment) combines this with
	// its own flag via super.dependsOnInput() rather than overriding this field directly.
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public AbstractBinaryOperatorExpression(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, String image) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.image = image;
		this.dependsOnInput = lhs.dependsOnInput() || rhs.dependsOnInput();
		this.dependsOnExternalState = lhs.dependsOnExternalState() || rhs.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.union(lhs, rhs);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(lhs, rhs);
	}

	public Expression<StackFrame, JsonNode> lhs() {
		return lhs;
	}

	public Expression<StackFrame, JsonNode> rhs() {
		return rhs;
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
	public String toString() {
		return String.format("(%s %s %s)", lhs, image, rhs);
	}
}
