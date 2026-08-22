package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Conditional<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<StackFrame, JsonNode> otherwise;
	private List<Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>>> switches;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	@Override
	public Cardinality getCardinality() {
		if (!switches.isEmpty() && switches.get(0)._1.getCardinality() == Cardinality.ZERO)
			return Cardinality.ZERO;
		for (Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>> sw : switches) {
			if (sw._1.getCardinality() != Cardinality.ONE)
				return Cardinality.UNKNOWN;
		}
		Cardinality expected = otherwise != null ? otherwise.getCardinality() : Cardinality.ZERO;
		if (expected == Cardinality.UNKNOWN)
			return Cardinality.UNKNOWN;
		for (Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>> sw : switches) {
			if (sw._2.getCardinality() != expected)
				return Cardinality.UNKNOWN;
		}
		return expected;
	}

	public Conditional(JsonProvider<JsonNode> jsonProvider, List<Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>>> switches, Expression<StackFrame, JsonNode> otherwise) {
		this.jsonProvider = jsonProvider;
		this.switches = switches;
		this.otherwise = otherwise;
		@Var boolean anyDependsOnInput = otherwise.dependsOnInput();
		@Var boolean anyDependsOnExternalState = otherwise.dependsOnExternalState();
		@Var boolean anyOpaque = FreeVariables.anyOpaque(otherwise);
		Set<Integer> slots = new HashSet<>(FreeVariables.slotsOf(otherwise));
		for (Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>> sw : switches) {
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

	public List<Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>>> switches() {
		return switches;
	}

	public Expression<StackFrame, JsonNode> otherwise() {
		return otherwise;
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
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		applyBranch(frame, in, path, output, 0);
	}

	private void applyBranch(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output, int switchIndex) throws JsonQueryException {
		if (switchIndex >= switches.size()) {
			if (otherwise != null) {
				otherwise.apply(frame, in, path, output);
			}
			return;
		}
		Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>> sw = switches.get(switchIndex);
		List<JsonNode> condValues = new ArrayList<>();
		sw._1.apply(frame, in, null, (r, opath) -> condValues.add(r));

		for (JsonNode r : condValues) {
			if (JsonNodeUtils.asBoolean(jsonProvider, r)) {
				sw._2.apply(frame, in, path, output);
			} else {
				applyBranch(frame, in, path, output, switchIndex + 1);
			}
		}
	}

	@Override
	public String toString() {
		@Var String ifstr = "if";
		StringBuilder builder = new StringBuilder();
		for (Pair<Expression<StackFrame, JsonNode>, Expression<StackFrame, JsonNode>> sw : switches) {
			builder.append(ifstr);
			builder.append(" ");
			builder.append(sw._1 != null ? sw._1 : "null");
			builder.append(" ");
			builder.append("then");
			builder.append(" ");
			builder.append(sw._2 != null ? sw._2 : "null");
			builder.append(" ");
			ifstr = "elif";
		}
		builder.append("else ");
		builder.append(otherwise != null ? otherwise : "null");
		builder.append(" ");
		builder.append("end");
		return builder.toString();
	}
}
