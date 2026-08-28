package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ResolvedLocalVariableAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final int slot;

	public ResolvedLocalVariableAccess(String name, int slot) {
		this.name = name;
		this.slot = slot;
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	@Override
	public boolean dependsOnInput() {
		return false;
	}

	@Override
	public boolean dependsOnExternalState() {
		return false;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.singleton(slot);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return false;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		PathAndValue<JsonNode> val = StackFrameValues.asPathAndValue(frame.get(slot));
		if (val != null && val.getValue() != null) {
			output.emit(val.getValue(), path instanceof UntrackedPath ? UntrackedPath.getInstance() : val.getPath());
			return;
		}
		throw new JsonQueryException(String.format("$%s is not defined", name));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
