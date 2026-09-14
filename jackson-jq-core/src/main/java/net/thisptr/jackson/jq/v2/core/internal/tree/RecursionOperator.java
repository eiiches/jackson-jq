package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RecursionOperator<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final boolean dependsOnInput;
	private final boolean visitsNullValues;

	public RecursionOperator(JsonProvider<JsonNode> jsonProvider, boolean dependsOnInput, boolean visitsNullValues) {
		this.jsonProvider = jsonProvider;
		this.dependsOnInput = dependsOnInput;
		this.visitsNullValues = visitsNullValues;
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

	private void pathRecursive(JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		output.emit(in, path);
		if (jsonProvider.isObject(in)) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				if (visitsNullValues || !jsonProvider.isNull(entry.getValue()))
					pathRecursive(entry.getValue(), path.appendKey(entry.getKey()), output);
			}
		} else if (jsonProvider.isArray(in)) {
			for (int i = 0; i < jsonProvider.getArrayLength(in); ++i) {
				JsonNode element = jsonProvider.getArrayElement(in, i);
				if (visitsNullValues || !jsonProvider.isNull(element))
					pathRecursive(element, path.appendIndex(i), output);
			}
		}
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		pathRecursive(in, path, output);
	}
}
