package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class VariableBinding<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final AnalyzedExpression<JsonNode> value;
	private final PatternMatcher<JsonNode> matcher;
	private final AnalyzedExpression<JsonNode> body;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;
	private final Set<Integer> boundSlots;
	// Counter for the bound expression. `body` needs none: it emits this binding's own values.
	private final int valueOutputIndex;

	public VariableBinding(AnalyzedExpression<JsonNode> value, PatternMatcher<JsonNode> matcher, Set<Integer> boundSlots, AnalyzedExpression<JsonNode> body, int valueOutputIndex) {
		this.valueOutputIndex = valueOutputIndex;
		this.boundSlots = boundSlots;
		this.value = value;
		this.matcher = matcher;
		this.body = body;
		this.dependsOnInput = value.dependsOnInput() || body.dependsOnInput();
		this.dependsOnExternalState = value.dependsOnExternalState() || matcher.dependsOnExternalState() || body.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.unionSets(FreeVariables.slotsOf(value), FreeVariables.minus(FreeVariables.slotsOf(body), boundSlots));
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(value, body);
	}

	public AnalyzedExpression<JsonNode> value() {
		return value;
	}

	public AnalyzedExpression<JsonNode> body() {
		return body;
	}

	public PatternMatcher<JsonNode> matcher() {
		return matcher;
	}

	public Set<Integer> boundSlots() {
		return boundSlots;
	}

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(value.getCardinality(), matcher.getCardinality(), body.getCardinality());
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
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> rewrittenValue = rewriter.rewrite(value);
		PatternMatcher<JsonNode> rewrittenMatcher = matcher.rewriteExpressions(rewriter::rewrite);
		AnalyzedExpression<JsonNode> rewrittenBody = rewriter.rewrite(body);
		return rewrittenValue == value && rewrittenMatcher == matcher && rewrittenBody == body
				? this
				: new VariableBinding<>(rewrittenValue, rewrittenMatcher, boundSlots, rewrittenBody, valueOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		// The matcher binds its variables straight into the frame, so by the time onMatch runs the body
		// can simply read them.
		PatternMatcher.OnMatch onMatch = () -> body.apply(frame, in, path, output);
		Memory memory = frame.getEnclosingMemory();
		value.apply(frame, in, UntrackedPath.getInstance(),
				(matchedValue, ignoredPath) -> {
					memory.countOutput(valueOutputIndex);
					matcher.matchWithPath(frame, matchedValue, path, onMatch);
				});
	}
}
