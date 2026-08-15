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
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "paths", nargs = 1)
public class PathsFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, ipath, output) -> {
			Stack<JsonNode> stack = new Stack<>();
			applyRecursive(scope, jsonProvider, in, output, stack, args.get(0));
		};
	}

	private static <JsonNode> void applyRecursive(Scope<JsonNode> scope, JsonProvider<JsonNode> jsonProvider, JsonNode in, PathOutput<JsonNode> output, Stack<JsonNode> stack, Expression predicate) throws JsonQueryException {
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
				stack.push(jsonProvider.createNumber(i));
				applyRecursive(scope, jsonProvider, jsonProvider.requireGet(in, i), output, stack, predicate);
				stack.pop();
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
				stack.push(jsonProvider.createString(entry.getKey()));
				applyRecursive(scope, jsonProvider, entry.getValue(), output, stack, predicate);
				stack.pop();
			}
		}
	}
}
