package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ReduceExpression<JsonNode> implements Expression {
	private Expression iterExpr;
	private Expression reduceExpr;
	private Expression initExpr;
	private PatternMatcher<JsonNode> matcher;
	private java.util.Map<String, Integer> slots;

	public ReduceExpression(PatternMatcher<JsonNode> matcher, Expression initExpr, Expression reduceExpr, Expression iterExpr) {
		this(matcher, initExpr, reduceExpr, iterExpr, java.util.Collections.emptyMap());
	}

	public ReduceExpression(PatternMatcher<JsonNode> matcher, Expression initExpr, Expression reduceExpr, Expression iterExpr, java.util.Map<String, Integer> slots) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
		this.slots = slots;
	}

	public PatternMatcher<JsonNode> matcher() { return matcher; }
	public Expression initExpr() { return initExpr; }
	public Expression reduceExpr() { return reduceExpr; }
	public Expression iterExpr() { return iterExpr; }

	public int getSlot(String name) {
		Integer slot = slots.get(name);
		return slot != null ? slot.intValue() : -1;
	}

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public <N> void apply(JsonProvider<N> jsonProvider, ExecutionStack<N>.@Nullable Frame frame, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		applyInternal((JsonProvider) jsonProvider, (ExecutionStack.Frame) frame, (JsonNode) in, (PathOutput) output);
	}

	private void applyInternal(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, PathOutput<JsonNode> output) throws JsonQueryException {
		initExpr.apply(jsonProvider, frame, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };

			iterExpr.apply(jsonProvider, frame, in, (item) -> {
				Stack<Pair<String, JsonNode>> stack = new Stack<>();
				matcher.match(jsonProvider, frame, item, (List<Pair<String, JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						Pair<String, JsonNode> var = vars.get(i);
						int slot = getSlot(var._1);
						if (frame != null && slot >= 0) {
							frame.set(slot, var._2);
						}
					}

					// We only use the last value from reduce expression.
					List<JsonNode> reduceResult = new ArrayList<>();
					reduceExpr.apply(jsonProvider, frame, accumulators[0], reduceResult::add);
					accumulators[0] = reduceResult.isEmpty() ? jsonProvider.createNull() : reduceResult.get(reduceResult.size() - 1);
				}, stack);
			});

			output.emit(accumulators[0], null);
		});
	}

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}
