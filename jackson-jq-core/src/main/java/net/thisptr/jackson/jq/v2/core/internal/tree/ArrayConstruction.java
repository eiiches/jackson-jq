package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ArrayConstruction<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	public final @Nullable Expression<StackFrame, JsonNode> q;
	private final int qOutputIndex;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ArrayConstruction(JsonProvider<JsonNode> jsonProvider) {
		this(jsonProvider, null, Memory.NO_OUTPUT_COUNTER);
	}

	public ArrayConstruction(JsonProvider<JsonNode> jsonProvider, @Nullable Expression<StackFrame, JsonNode> q, int qOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.q = q;
		this.qOutputIndex = qOutputIndex;
		this.dependsOnInput = q != null && q.dependsOnInput();
		this.dependsOnExternalState = q != null && q.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.union(q);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(q);
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		List<JsonNode> values = new ArrayList<>();
		if (q != null) {
			RuntimeLimits limits = frame.getRuntimeLimits();
			Memory memory = frame.getEnclosingMemory();
			q.apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> {
				memory.countOutput(qOutputIndex);
				RuntimeLimitChecks.checkArraySize(limits, values.size() + 1L);
				values.add(out);
			});
		}
		output.emit(jsonProvider.createArray(values), UntrackedPath.getInstance());
	}
}
