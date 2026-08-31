package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RecursionOperator<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final boolean dependsOnInput;

	public RecursionOperator(JsonProvider<JsonNode> jsonProvider, boolean dependsOnInput) {
		this.jsonProvider = jsonProvider;
		this.dependsOnInput = dependsOnInput;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return false;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return false;
	}

	private void pathRecursive(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		output.emit(in, path);
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectEntries(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				pathRecursive(frame, entry.getValue(), path.appendKey(entry.getKey()), output);
			}
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.getArrayLength(in); ++i)
				pathRecursive(frame, jsonProvider.getArrayElement(in, i), path.appendIndex(i), output);
		}
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		pathRecursive(frame, in, path, output);
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
