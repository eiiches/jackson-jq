package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Label<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final Expression<StackFrame, JsonNode> body;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public Label(String name, Expression<StackFrame, JsonNode> body) {
		this.name = name;
		this.body = body;
		this.dependsOnInput = body.dependsOnInput();
		this.dependsOnExternalState = body.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.slotsOf(body);
		this.hasOpaqueVariableReference = FreeVariables.opaqueIn(body);
	}

	@Override
	public Cardinality getCardinality() {
		return body.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN;
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		try {
			body.apply(frame, in, path, output);
		} catch (JsonQueryBreakException e) {
			if (!name.equals(e.name()))
				throw e;
		}
	}
}
