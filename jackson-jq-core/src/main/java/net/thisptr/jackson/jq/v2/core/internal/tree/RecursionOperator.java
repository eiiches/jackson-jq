package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Iterator;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RecursionOperator implements Expression, AstNode {
	private static <JsonNode> void pathRecursive(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		output.emit(in, path);
		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				pathRecursive(jsonProvider, frame, entry.getValue(), ObjectFieldPath.chainIfNotNull(path, entry.getKey()), output);
			}
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			for (int i = 0; i < jsonProvider.size(in); ++i)
				pathRecursive(jsonProvider, frame, jsonProvider.requireGet(in, i), ArrayIndexPath.chainIfNotNull(jsonProvider, path, i), output);
		}
	}

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(jsonProvider, frame, in, path, output);
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
