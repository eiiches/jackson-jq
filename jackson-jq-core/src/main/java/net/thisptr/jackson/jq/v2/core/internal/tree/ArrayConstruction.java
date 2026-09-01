package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ArrayConstruction<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	public final @Nullable Expression<StackFrame, JsonNode> q;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ArrayConstruction(JsonProvider<JsonNode> jsonProvider) {
		this(jsonProvider, null);
	}

	public ArrayConstruction(JsonProvider<JsonNode> jsonProvider, @Nullable Expression<StackFrame, JsonNode> q) {
		this.jsonProvider = jsonProvider;
		this.q = q;
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
		if (q != null)
			q.apply(frame, in, UntrackedPath.getInstance(), (out, opath) -> values.add(out));
		output.emit(jsonProvider.createArray(values), UntrackedPath.getInstance());
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
