package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "indices", nargs = 1)
public class IndicesFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "indices", in, JsonNodeType.STRING, JsonNodeType.ARRAY, JsonNodeType.NULL);

		if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), null);
			return;
		}

		args.get(0).apply(scope, in, (needle) -> {
			JsonNode indices = jsonProvider.createArray();
			for (int index : indices(jsonProvider, needle, in))
				jsonProvider.add(indices, jsonProvider.createNumber(index));
			output.emit(indices, null);
		});
	}

	public static <JsonNode> List<Integer> indices(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) throws JsonQueryException {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		List<Integer> result = new ArrayList<>();
		JsonNodeType needleType = jsonProvider.getNodeType(needle);
		JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING) {
			String haystackText = jsonProvider.asText(haystack);
			String needleText = jsonProvider.asText(needle);
			if (!needleText.isEmpty()) {
				for (int index = haystackText.indexOf(needleText); index >= 0; index = haystackText.indexOf(needleText, index + 1))
					result.add(index);
			}
		} else if (needleType == JsonNodeType.ARRAY && haystackType == JsonNodeType.ARRAY) {
			int needleSize = jsonProvider.size(needle);
			int haystackSize = jsonProvider.size(haystack);
			if (needleSize != 0) {
				shift: for (int i = 0; i < haystackSize - needleSize + 1; ++i) {
					for (int j = 0; j < needleSize; ++j)
						if (comparator.compare(jsonProvider.requireGet(haystack, i + j), jsonProvider.requireGet(needle, j)) != 0)
							continue shift;
					result.add(i);
				}
			}
		} else if (haystackType == JsonNodeType.ARRAY) {
			int haystackSize = jsonProvider.size(haystack);
			for (int i = 0; i < haystackSize; ++i)
				if (comparator.compare(jsonProvider.requireGet(haystack, i), needle) == 0)
					result.add(i);
		} else {
			throw new JsonQueryException("indices() is not applicable to " + haystackType);
		}
		return result;
	}
}
