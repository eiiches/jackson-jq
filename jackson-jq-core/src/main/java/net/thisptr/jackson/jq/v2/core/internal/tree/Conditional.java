package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class Conditional<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final AnalyzedExpression<JsonNode> otherwise;
	private final List<Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>>> switches;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;
	// One counter per condition. The branches need none: whichever one runs emits this expression's own
	// values straight to `output`, so they are charged wherever this conditional's own values are.
	private final int[] conditionOutputIndices;

	@Override
	public Cardinality getCardinality() {
		if (!switches.isEmpty() && switches.get(0)._1.getCardinality() == Cardinality.ZERO)
			return Cardinality.ZERO;
		for (Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>> sw : switches) {
			if (sw._1.getCardinality() != Cardinality.ONE)
				return Cardinality.UNKNOWN;
		}
		Cardinality expected = otherwise != null ? otherwise.getCardinality() : Cardinality.ZERO;
		if (expected == Cardinality.UNKNOWN)
			return Cardinality.UNKNOWN;
		for (Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>> sw : switches) {
			if (sw._2.getCardinality() != expected)
				return Cardinality.UNKNOWN;
		}
		return expected;
	}

	public Conditional(JsonProvider<JsonNode> jsonProvider, List<Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>>> switches, AnalyzedExpression<JsonNode> otherwise, int[] conditionOutputIndices) {
		this.conditionOutputIndices = conditionOutputIndices;
		this.jsonProvider = jsonProvider;
		this.switches = switches;
		this.otherwise = otherwise;
		@Var boolean anyDependsOnInput = otherwise.dependsOnInput();
		@Var boolean anyDependsOnExternalState = otherwise.dependsOnExternalState();
		@Var boolean anyOpaque = FreeVariables.anyOpaque(otherwise);
		Set<Integer> slots = new HashSet<>(FreeVariables.slotsOf(otherwise));
		for (Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>> sw : switches) {
			anyDependsOnInput = anyDependsOnInput || sw._1.dependsOnInput() || sw._2.dependsOnInput();
			anyDependsOnExternalState = anyDependsOnExternalState || sw._1.dependsOnExternalState() || sw._2.dependsOnExternalState();
			anyOpaque = anyOpaque || FreeVariables.anyOpaque(sw._1, sw._2);
			slots.addAll(FreeVariables.slotsOf(sw._1));
			slots.addAll(FreeVariables.slotsOf(sw._2));
		}
		this.dependsOnInput = anyDependsOnInput;
		this.dependsOnExternalState = anyDependsOnExternalState;
		this.hasOpaqueVariableReference = anyOpaque;
		this.freeLocalSlots = slots;
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
		@Var List<Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>>> rewrittenSwitches = null;
		for (int i = 0; i < switches.size(); i++) {
			Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>> sw = switches.get(i);
			AnalyzedExpression<JsonNode> condition = rewriter.rewrite(sw._1);
			AnalyzedExpression<JsonNode> branch = rewriter.rewrite(sw._2);
			if (rewrittenSwitches == null && (condition != sw._1 || branch != sw._2))
				rewrittenSwitches = new ArrayList<>(switches);
			if (rewrittenSwitches != null)
				rewrittenSwitches.set(i, Pair.of(condition, branch));
		}
		AnalyzedExpression<JsonNode> rewrittenOtherwise = rewriter.rewrite(otherwise);
		return rewrittenSwitches == null && rewrittenOtherwise == otherwise
				? this
				: new Conditional<>(jsonProvider, rewrittenSwitches != null ? rewrittenSwitches : switches, rewrittenOtherwise, conditionOutputIndices);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		applyBranch(frame, in, path, output, 0);
	}

	private void applyBranch(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output, int switchIndex) throws JsonQueryException {
		if (switchIndex >= switches.size()) {
			if (otherwise != null) {
				otherwise.apply(frame, in, path, output);
			}
			return;
		}
		Pair<AnalyzedExpression<JsonNode>, AnalyzedExpression<JsonNode>> sw = switches.get(switchIndex);
		List<JsonNode> condValues = new ArrayList<>();
		Memory memory = frame.getEnclosingMemory();
		sw._1.apply(frame, in, UntrackedPath.getInstance(), (r, opath) -> {
			memory.countOutput(conditionOutputIndices[switchIndex]);
			condValues.add(r);
		});
		for (JsonNode r : condValues) {
			if (JsonNodeUtils.asBoolean(jsonProvider, r)) {
				sw._2.apply(frame, in, path, output);
			} else {
				applyBranch(frame, in, path, output, switchIndex + 1);
			}
		}
	}
}
