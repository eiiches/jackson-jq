package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "paths", nargs = 1)
public class PathsFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			List<JsonNode> stack = new ArrayList<>();
			applyRecursive(frame, jsonProvider, in, output, stack, args.get(0));
		};
	}

	private static <JsonNode> void applyRecursive(@Nullable StackFrame frame, JsonProvider<JsonNode> jsonProvider, JsonNode in, Output<JsonNode> output, List<JsonNode> stack, Expression<JsonNode> predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(frame, in, null, (shouldInclude, opath) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), null);
			});
		}

		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			int size = jsonProvider.size(in);
			for (int i = 0; i < size; ++i) {
				stack.add(jsonProvider.createNumber(i));
				applyRecursive(frame, jsonProvider, jsonProvider.requireGet(in, i), output, stack, predicate);
				stack.remove(stack.size() - 1);
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				stack.add(jsonProvider.createString(entry.getKey()));
				applyRecursive(frame, jsonProvider, entry.getValue(), output, stack, predicate);
				stack.remove(stack.size() - 1);
			}
		}
	}
}
