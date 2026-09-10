package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
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
@FunctionRegistration(name = "indices", nargs = 1)
public class IndicesFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> emitIndices(jsonProvider, needle, in, version, output));
		});
	}

	static <JsonNode> void emitIndices(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack, Version version, Output<JsonNode> output) throws JsonQueryException {
		JsonNodeType needleType = jsonProvider.getNodeType(needle);
		JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (haystackType == JsonNodeType.ARRAY || (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING)) {
			List<JsonNode> result = new ArrayList<>();
			for (int index : findIndices(jsonProvider, needle, haystack))
				result.add(jsonProvider.createNumber(index));
			output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
		} else if (needleType == JsonNodeType.STRING) {
			PathOperations.resolveObjectField(jsonProvider, haystack, UntrackedPath.getInstance(), output, jsonProvider.getString(needle), false, version);
		} else if (needleType == JsonNodeType.NUMBER) {
			PathOperations.resolveArrayIndex(jsonProvider, haystack, UntrackedPath.getInstance(), output, needle, false, version);
		} else if (needleType == JsonNodeType.ARRAY) {
			PathOperations.resolveArrayIndexOf(jsonProvider, haystack, UntrackedPath.getInstance(), output, needle, false, version);
		} else {
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, haystack, needle));
		}
	}

	private static <JsonNode> List<Integer> findIndices(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		List<Integer> result = new ArrayList<>();
		JsonNodeType needleType = jsonProvider.getNodeType(needle);
		JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING) {
			String haystackText = jsonProvider.getString(haystack);
			String needleText = jsonProvider.getString(needle);
			if (!needleText.isEmpty()) {
				for (int index = haystackText.indexOf(needleText); index >= 0; index = haystackText.indexOf(needleText, index + 1))
					result.add(index);
			}
		} else if (needleType == JsonNodeType.ARRAY && haystackType == JsonNodeType.ARRAY) {
			int needleSize = jsonProvider.getArrayLength(needle);
			int haystackSize = jsonProvider.getArrayLength(haystack);
			if (needleSize != 0) {
				shift:
				for (int i = 0; i < haystackSize - needleSize + 1; ++i) {
					for (int j = 0; j < needleSize; ++j)
						if (comparator.compare(jsonProvider.getArrayElement(haystack, i + j), jsonProvider.getArrayElement(needle, j)) != 0)
							continue shift;
					result.add(i);
				}
			}
		} else if (haystackType == JsonNodeType.ARRAY) {
			int haystackSize = jsonProvider.getArrayLength(haystack);
			for (int i = 0; i < haystackSize; ++i)
				if (comparator.compare(jsonProvider.getArrayElement(haystack, i), needle) == 0)
					result.add(i);
		}
		return result;
	}
}
