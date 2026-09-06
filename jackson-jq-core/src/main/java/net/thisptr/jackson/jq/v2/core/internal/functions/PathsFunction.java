package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "paths", nargs = 1)
public class PathsFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			List<JsonNode> stack = new ArrayList<>();
			applyRecursive(frame, jsonProvider, in, output, stack, args.get(0));
		});
	}

	private static <Context, JsonNode> void applyRecursive(Context context, JsonProvider<JsonNode> jsonProvider, JsonNode in, Output<JsonNode> output, List<JsonNode> stack, Expression<Context, JsonNode> predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(context, in, UntrackedPath.getInstance(), (shouldInclude, opath) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), UntrackedPath.getInstance());
			});
		}

		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			int size = jsonProvider.getArrayLength(in);
			for (int i = 0; i < size; ++i) {
				stack.add(jsonProvider.createNumber(i));
				applyRecursive(context, jsonProvider, jsonProvider.getArrayElement(in, i), output, stack, predicate);
				stack.remove(stack.size() - 1);
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				stack.add(jsonProvider.createString(entry.getKey()));
				applyRecursive(context, jsonProvider, entry.getValue(), output, stack, predicate);
				stack.remove(stack.size() - 1);
			}
		}
	}
}
