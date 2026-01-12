package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("paths/1")
public class PathsFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final Stack<JsonNode> stack = new Stack<>();
		applyRecursive(jsonProvider, scope, in, output, stack, args.get(0));
	}

	private void applyRecursive(final JsonProvider<JsonNode> jsonProvider, final Scope<JsonNode> scope, final JsonNode in, final PathOutput<JsonNode> output, final Stack<JsonNode> stack, final Expression<JsonNode> predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(scope, in, (shouldInclude) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), null);
			});
		}

		final JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			final int size = jsonProvider.size(in);
			for (int i = 0; i < size; ++i) {
				stack.push(jsonProvider.createInt(i));
				applyRecursive(jsonProvider, scope, jsonProvider.get(in, i), output, stack, predicate);
				stack.pop();
			}
		} else if (inType == JsonNodeType.OBJECT) {
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				stack.push(jsonProvider.createString(entry.getKey()));
				applyRecursive(jsonProvider, scope, entry.getValue(), output, stack, predicate);
				stack.pop();
			}
		}
	}
}
