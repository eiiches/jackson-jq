package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("paths/1")
public class PathsFunction<JsonNode> implements Function {
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> scope, List<Expression<N>> args, N in, Path<N> ipath, PathOutput<N> output, Version version) throws JsonQueryException {
		applyInternal((Scope) scope, (List) args, (JsonNode) in, (Path) ipath, (PathOutput) output, version);
	}

	private void applyInternal(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Stack<JsonNode> stack = new Stack<>();
		applyRecursive(jsonProvider, scope, in, output, stack, args.get(0));
	}

	private void applyRecursive(JsonProvider<JsonNode> jsonProvider, Scope<JsonNode> scope, JsonNode in, PathOutput<JsonNode> output, Stack<JsonNode> stack, Expression<JsonNode> predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(scope, in, (shouldInclude) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), null);
			});
		}

		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			int size = jsonProvider.size(in);
			for (int i = 0; i < size; ++i) {
				stack.push(jsonProvider.createInt(i));
				applyRecursive(jsonProvider, scope, jsonProvider.get(in, i), output, stack, predicate);
				stack.pop();
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				stack.push(jsonProvider.createString(entry.getKey()));
				applyRecursive(jsonProvider, scope, entry.getValue(), output, stack, predicate);
				stack.pop();
			}
		}
	}
}
