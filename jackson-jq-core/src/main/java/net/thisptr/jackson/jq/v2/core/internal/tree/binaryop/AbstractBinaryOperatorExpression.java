package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;

public abstract class AbstractBinaryOperatorExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	protected final AnalyzedExpression<JsonNode> lhs;
	protected final AnalyzedExpression<JsonNode> rhs;
	// Counters for the two operand streams. Every operator here evaluates both operands through sinks of
	// its own -- to form the cross product, to test truthiness, to collect paths -- so both are charged
	// here rather than by whatever consumes the operator's own result.
	protected final int lhsOutputIndex;
	protected final int rhsOutputIndex;
	// Default `lhs || rhs` formulas shared by every non-assignment operator (arithmetic, comparison,
	// and/or, //). The assignment family (whose dependsOnInput additionally depends on whether `.`
	// itself is known fixed -- see Assignment/AbstractComplexAssignment/UpdateAssignment) combines this with
	// its own flag via super.dependsOnInput() rather than overriding this field directly.
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public AbstractBinaryOperatorExpression(AnalyzedExpression<JsonNode> lhs, AnalyzedExpression<JsonNode> rhs, int lhsOutputIndex, int rhsOutputIndex) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.lhsOutputIndex = lhsOutputIndex;
		this.rhsOutputIndex = rhsOutputIndex;
		this.dependsOnInput = lhs.dependsOnInput() || rhs.dependsOnInput();
		this.dependsOnExternalState = lhs.dependsOnExternalState() || rhs.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.union(lhs, rhs);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(lhs, rhs);
	}

	public AnalyzedExpression<JsonNode> lhs() {
		return lhs;
	}

	public AnalyzedExpression<JsonNode> rhs() {
		return rhs;
	}

	protected abstract AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenLhs, AnalyzedExpression<JsonNode> rewrittenRhs);

	@Override
	public final AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> rewrittenLhs = rewriter.rewrite(lhs);
		AnalyzedExpression<JsonNode> rewrittenRhs = rewriter.rewrite(rhs);
		return rewrittenLhs == lhs && rewrittenRhs == rhs ? this : recreate(rewrittenLhs, rewrittenRhs);
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
}
