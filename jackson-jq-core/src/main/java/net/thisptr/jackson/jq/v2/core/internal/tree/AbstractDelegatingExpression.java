package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Base class for a wrapper the compiler puts around an already-compiled node, forwarding every question
 * the compiler goes on to ask of it.
 * <p>
 * Every such question has to reach the node underneath, or wrapping would change how the rest of the
 * query compiles: {@link FreeVariables#dependsOnVariables} reads an expression that does not implement
 * {@link FreeVariables} as depending on variables, which silently disables constant folding, and
 * {@code Cardinality}/{@code dependsOn*} drive output metering and constant folding the same way.
 * Extending this rather than implementing {@link Expression} directly is what keeps a new wrapper from
 * dropping one of them.
 * <p>
 * A subclass overrides {@link #apply} when it has something to do at evaluation time, and overrides an
 * individual fact only where it genuinely differs from the wrapped node -- as
 * {@link MeteredConstantOutputExpression} does for {@code getConstantResults()}.
 *
 * @param <JsonNode> the JSON node type
 */
public abstract class AbstractDelegatingExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	protected final AnalyzedExpression<JsonNode> inner;

	protected AbstractDelegatingExpression(AnalyzedExpression<JsonNode> inner) {
		this.inner = inner;
	}

	/**
	 * Returns the wrapped expression.
	 *
	 * @return the wrapped expression, never {@code null}
	 */
	public final AnalyzedExpression<JsonNode> inner() {
		return inner;
	}

	protected abstract AnalyzedExpression<JsonNode> recreate(AnalyzedExpression<JsonNode> rewrittenInner);

	@Override
	public final AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> rewritten = rewriter.rewrite(inner);
		return rewritten == inner ? this : recreate(rewritten);
	}

	@Override
	public Cardinality getCardinality() {
		return inner.getCardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return inner.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return inner.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.slotsOf(inner);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.opaqueIn(inner);
	}

	@Override
	public void apply(StackFrame context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		inner.apply(context, in, ipath, output);
	}
}
