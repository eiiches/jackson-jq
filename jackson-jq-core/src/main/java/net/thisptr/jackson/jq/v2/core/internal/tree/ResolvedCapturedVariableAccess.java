package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.compile.Closure;
import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

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
	@SuppressWarnings("unchecked")
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Closure closure = (Closure) frame.get(frameClosureSlot);
		if (closure == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined (no closure)");
		}
		Object raw = closure.get(closureSlot);
		if (raw == null) {
			throw new JsonQueryException("Variable $" + name + " is not defined");
		}
		if (raw instanceof PathAndValue) {
			PathAndValue<JsonNode> pv = (PathAndValue<JsonNode>) raw;
			if (pv.getValue() != null) {
				output.emit(pv.getValue(), path != null ? pv.getPath() : null);
			}
		} else {
			output.emit((JsonNode) raw, null);
		}
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
