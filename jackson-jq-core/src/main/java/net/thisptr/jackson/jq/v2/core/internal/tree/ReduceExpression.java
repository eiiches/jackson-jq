package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ReduceExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<StackFrame, JsonNode> iterExpr;
	private Expression<StackFrame, JsonNode> reduceExpr;
	private Expression<StackFrame, JsonNode> initExpr;
	private PatternMatcher<JsonNode> matcher;

	@Override
	public Cardinality getCardinality() {
		return initExpr.getCardinality();
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ReduceExpression(JsonProvider<JsonNode> jsonProvider, PatternMatcher<JsonNode> matcher, Expression<StackFrame, JsonNode> initExpr, Expression<StackFrame, JsonNode> reduceExpr, Expression<StackFrame, JsonNode> iterExpr, Set<Integer> matcherSlots) {
		this.jsonProvider = jsonProvider;
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
		// reduceExpr is already compiled under the correct shielded context (see Compiler's
		// ReduceExpressionAstNode handling), so dependsOnInput/dependsOnExternalState are a flat OR,
		// same as everywhere else. The matcher's bound slot(s) are only "closed" for reduceExpr --
		// they're not yet bound while initExpr/iterExpr run.
		this.dependsOnInput = initExpr.dependsOnInput() || iterExpr.dependsOnInput() || reduceExpr.dependsOnInput();
		this.dependsOnExternalState = initExpr.dependsOnExternalState() || iterExpr.dependsOnExternalState() || reduceExpr.dependsOnExternalState();
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(initExpr, iterExpr, reduceExpr);
		this.freeLocalSlots = FreeVariables.minus(
				FreeVariables.union(initExpr, iterExpr, reduceExpr),
				new ArrayList<>(matcherSlots));
	}

	public PatternMatcher<JsonNode> matcher() {
		return matcher;
	}

	public Expression<StackFrame, JsonNode> initExpr() {
		return initExpr;
	}

	public Expression<StackFrame, JsonNode> reduceExpr() {
		return reduceExpr;
	}

	public Expression<StackFrame, JsonNode> iterExpr() {
		return iterExpr;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		initExpr.apply(frame, in, UntrackedPath.getInstance(), (accumulator, opath) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			iterExpr.apply(frame, in, UntrackedPath.getInstance(), (item, opath2) -> {
				Deque<PatternMatcher.Match<JsonNode>> stack = new ArrayDeque<>();
				matcher.match(frame, item, (Deque<PatternMatcher.Match<JsonNode>> vars) -> {
					for (Iterator<PatternMatcher.Match<JsonNode>> it = vars.descendingIterator(); it.hasNext(); ) {
						PatternMatcher.Match<JsonNode> var = it.next();
						if (var.slot >= 0) {
							frame.set(var.slot, var.value);
						}
					}
					// We only use the last value from reduce expression.
					List<JsonNode> reduceResult = new ArrayList<>();
					reduceExpr.apply(frame, accumulators[0], UntrackedPath.getInstance(), (v, opath3) -> reduceResult.add(v));
					accumulators[0] = reduceResult.isEmpty() ? jsonProvider.createNull() : reduceResult.get(reduceResult.size() - 1);
				}, stack);
			});
			output.emit(accumulators[0], UntrackedPath.getInstance());
		});
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}
