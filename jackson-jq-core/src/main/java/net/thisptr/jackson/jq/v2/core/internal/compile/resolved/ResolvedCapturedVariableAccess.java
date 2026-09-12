package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ResolvedCapturedVariableAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final int closureSlot;
	private final int frameClosureSlot;

	public ResolvedCapturedVariableAccess(String name, int closureSlot, int frameClosureSlot) {
		this.name = name;
		this.closureSlot = closureSlot;
		this.frameClosureSlot = frameClosureSlot;
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
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

	// Crosses a def/closure boundary -- never subtractable by a local `as`/reduce/foreach binding
	// within the current frame ("defs stay conservative").
	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return true;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Closure closure = (Closure) frame.get(frameClosureSlot);
		if (closure == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined (no closure)");
		}
		PathAndValue<JsonNode> pv = StackFrameValues.asPathAndValue(closure.get(closureSlot));
		if (pv == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined");
		}
		output.emit(pv.getValue(), path instanceof UntrackedPath ? UntrackedPath.getInstance() : pv.getPath());
	}
}
