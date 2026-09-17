package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
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
	private final Expression<StackFrame, JsonNode> iterExpr;
	private final Expression<StackFrame, JsonNode> reduceExpr;
	private final Expression<StackFrame, JsonNode> initExpr;
	private final PatternMatcher<JsonNode> matcher;
	private final int initOutputIndex;
	private final int reduceOutputIndex;
	private final int iterOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return initExpr.getCardinality();
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ReduceExpression(JsonProvider<JsonNode> jsonProvider, PatternMatcher<JsonNode> matcher, Expression<StackFrame, JsonNode> initExpr, Expression<StackFrame, JsonNode> reduceExpr, Expression<StackFrame, JsonNode> iterExpr, Set<Integer> matcherSlots, int initOutputIndex, int reduceOutputIndex, int iterOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.matcher = matcher;
		this.initOutputIndex = initOutputIndex;
		this.reduceOutputIndex = reduceOutputIndex;
		this.iterOutputIndex = iterOutputIndex;
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
		Memory memory = frame.getEnclosingMemory();
		initExpr.apply(frame, in, UntrackedPath.getInstance(), (accumulator, opath) -> {
			memory.countOutput(initOutputIndex);
			// Wrap in array to allow mutation inside lambda
			@SuppressWarnings("unchecked")
			JsonNode[] accumulators = (JsonNode[]) new Object[] { accumulator };
			// The matcher binds its variables straight into the frame, so by the time onMatch runs
			// reduceExpr can simply read them.
			PatternMatcher.OnMatch onMatch = () -> {
				// We only use the last value from reduce expression.
				List<JsonNode> reduceResult = new ArrayList<>();
				reduceExpr.apply(frame, accumulators[0], UntrackedPath.getInstance(), (v, opath3) -> {
					memory.countOutput(reduceOutputIndex);
					reduceResult.add(v);
				});
				accumulators[0] = reduceResult.isEmpty() ? jsonProvider.createNull() : reduceResult.get(reduceResult.size() - 1);
			};
			iterExpr.apply(frame, in, UntrackedPath.getInstance(), (item, opath2) -> {
				memory.countOutput(iterOutputIndex);
				matcher.match(frame, item, onMatch);
			});
			output.emit(accumulators[0], UntrackedPath.getInstance());
		});
	}
}
