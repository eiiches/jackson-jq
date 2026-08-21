package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ReduceExpression<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<JsonNode> iterExpr;
	private Expression<JsonNode> reduceExpr;
	private Expression<JsonNode> initExpr;
	private PatternMatcher<JsonNode> matcher;

	public ReduceExpression(JsonProvider<JsonNode> jsonProvider, PatternMatcher<JsonNode> matcher, Expression<JsonNode> initExpr, Expression<JsonNode> reduceExpr, Expression<JsonNode> iterExpr) {
		this.jsonProvider = jsonProvider;
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcher<JsonNode> matcher() { return matcher; }
	public Expression<JsonNode> initExpr() { return initExpr; }
	public Expression<JsonNode> reduceExpr() { return reduceExpr; }
	public Expression<JsonNode> iterExpr() { return iterExpr; }

	// reduce iterExpr as matcher (initExpr; reduceExpr)

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output) throws JsonQueryException {
		initExpr.apply(frame, in, (accumulator) -> {
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };

			iterExpr.apply(frame, in, (item) -> {
				Stack<PatternMatcher.Match<JsonNode>> stack = new Stack<>();
				matcher.match(frame, item, (List<PatternMatcher.Match<JsonNode>> vars) -> {
					for (int i = vars.size() - 1; i >= 0; --i) {
						PatternMatcher.Match<JsonNode> var = vars.get(i);
						if (frame != null && var.slot >= 0) {
							frame.set(var.slot, var.value);
						}
					}

					// We only use the last value from reduce expression.
					List<JsonNode> reduceResult = new ArrayList<>();
					reduceExpr.apply(frame, accumulators[0], reduceResult::add);
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
