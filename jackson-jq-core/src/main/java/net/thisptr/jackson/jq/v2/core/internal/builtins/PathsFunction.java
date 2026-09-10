package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
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
		/*
		 * jq redefined paths/1 in 1.7.1:
		 *
		 *   [1.5, 1.7.1): def paths(f): . as $dot | paths | select(getpath($dot; .) | f);
		 *   [1.7.1, ):    def paths(f): path(.. | select(f)) | select(length > 0);
		 *
		 * The newer definition evaluates f on the root value as well. The root path is empty, so
		 * select(length > 0) still keeps it out of the output; the only visible difference is that
		 * errors and side effects raised by f on the root now happen, before any child path is
		 * emitted.
		 */
		boolean appliesToRoot = version.compareTo(Versions.JQ_1_7_1) >= 0;
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			List<JsonNode> stack = new ArrayList<>();
			applyRecursive(frame, jsonProvider, in, output, stack, args.get(0), appliesToRoot);
		});
	}

	private static <Context, JsonNode> void applyRecursive(Context context, JsonProvider<JsonNode> jsonProvider, JsonNode in, Output<JsonNode> output, List<JsonNode> stack, Expression<Context, JsonNode> predicate, boolean appliesToRoot) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(context, in, UntrackedPath.getInstance(), (shouldInclude, opath) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), UntrackedPath.getInstance());
			});
		} else if (appliesToRoot) {
			predicate.apply(context, in, UntrackedPath.getInstance(), (shouldInclude, opath) -> {
				// The root is reached at the empty path, which is never part of the output.
			});
		}

		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			int size = jsonProvider.getArrayLength(in);
			for (int i = 0; i < size; ++i) {
				stack.add(jsonProvider.createNumber(i));
				applyRecursive(context, jsonProvider, jsonProvider.getArrayElement(in, i), output, stack, predicate, appliesToRoot);
				stack.remove(stack.size() - 1);
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				stack.add(jsonProvider.createString(entry.getKey()));
				applyRecursive(context, jsonProvider, entry.getValue(), output, stack, predicate, appliesToRoot);
				stack.remove(stack.size() - 1);
			}
		}
	}
}
