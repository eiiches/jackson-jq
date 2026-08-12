package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Iterator;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RecursionOperator<JsonNode> implements Expression<JsonNode> {
	private static <JsonNode> void pathRecursive(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		output.emit(in, path);
		if (scope.jsonProvider().getNodeType(in) == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> iter = scope.jsonProvider().fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				pathRecursive(scope, entry.getValue(), ObjectFieldPath.chainIfNotNull(path, entry.getKey()), output);
			}
		} else if (scope.jsonProvider().getNodeType(in) == JsonNodeType.ARRAY) {
			for (int i = 0; i < scope.jsonProvider().size(in); ++i)
				pathRecursive(scope, scope.jsonProvider().requireGet(in, i), ArrayIndexPath.chainIfNotNull(scope.jsonProvider(), path, i), output);
		}
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		pathRecursive(scope, in, path, output);
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
