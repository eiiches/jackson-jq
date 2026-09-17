package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class SemicolonOperator<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final List<Expression<StackFrame, JsonNode>> qs;

	@Override
	public Cardinality getCardinality() {
		return qs.isEmpty() ? Cardinality.ZERO : qs.get(qs.size() - 1).getCardinality();
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;
	private final Set<Integer> definedFunctionSlots;
	// One counter per discarded operand. The last one needs none: it emits this expression's own values.
	private final int[] discardedOutputIndices;

	public SemicolonOperator(List<Expression<StackFrame, JsonNode>> qs, int[] discardedOutputIndices) {
		this(qs, Collections.emptySet(), discardedOutputIndices);
	}

	public SemicolonOperator(List<Expression<StackFrame, JsonNode>> qs, Set<Integer> definedFunctionSlots, int[] discardedOutputIndices) {
		this.discardedOutputIndices = discardedOutputIndices;
		this.definedFunctionSlots = definedFunctionSlots;
		this.qs = qs;
		this.dependsOnInput = qs.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = qs.stream().anyMatch(Expression::dependsOnExternalState);
		this.freeLocalSlots = FreeVariables.minus(FreeVariables.unionAll(qs), definedFunctionSlots);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaqueIn(qs);
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
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		List<Expression<StackFrame, JsonNode>> rewritten = ExpressionRewriter.rewriteAll(qs, rewriter);
		return rewritten == qs ? this : new SemicolonOperator<>(rewritten, definedFunctionSlots, discardedOutputIndices);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		if (qs.isEmpty())
			return;
		Memory memory = frame.getEnclosingMemory();
		for (int i = 0; i < qs.size() - 1; ++i) {
			int discardedOutputIndex = discardedOutputIndices[i];
			qs.get(i).apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> memory.countOutput(discardedOutputIndex));
		}
		qs.get(qs.size() - 1).apply(frame, in, path, output);
	}
}
