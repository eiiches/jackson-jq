package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

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
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("indices/1")
public class IndicesFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "indices", in, JsonNodeType.STRING, JsonNodeType.ARRAY, JsonNodeType.NULL);

		if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
			output.emit(jsonProvider.createNull(), null);
			return;
		}

		args.get(0).apply(scope, in, (needle) -> {
			final JsonNode indices = jsonProvider.createArray();
			for (final int index : indices(jsonProvider, needle, in))
				jsonProvider.add(indices, jsonProvider.createInt(index));
			output.emit(indices, null);
		});
	}

	public static <JsonNode> List<Integer> indices(final JsonProvider<JsonNode> jsonProvider, final JsonNode needle, final JsonNode haystack) throws JsonQueryException {
		final JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		final List<Integer> result = new ArrayList<>();
		final JsonNodeType needleType = jsonProvider.getNodeType(needle);
		final JsonNodeType haystackType = jsonProvider.getNodeType(haystack);
		if (needleType == JsonNodeType.STRING && haystackType == JsonNodeType.STRING) {
			final String haystackText = jsonProvider.asText(haystack);
			final String needleText = jsonProvider.asText(needle);
			if (!needleText.isEmpty()) {
				for (int index = haystackText.indexOf(needleText); index >= 0; index = haystackText.indexOf(needleText, index + 1))
					result.add(index);
			}
		} else if (needleType == JsonNodeType.ARRAY && haystackType == JsonNodeType.ARRAY) {
			final int needleSize = jsonProvider.size(needle);
			final int haystackSize = jsonProvider.size(haystack);
			if (needleSize != 0) {
				shift: for (int i = 0; i < haystackSize - needleSize + 1; ++i) {
					for (int j = 0; j < needleSize; ++j)
						if (comparator.compare(jsonProvider.get(haystack, i + j), jsonProvider.get(needle, j)) != 0)
							continue shift;
					result.add(i);
				}
			}
		} else if (haystackType == JsonNodeType.ARRAY) {
			final int haystackSize = jsonProvider.size(haystack);
			for (int i = 0; i < haystackSize; ++i)
				if (comparator.compare(jsonProvider.get(haystack, i), needle) == 0)
					result.add(i);
		} else {
			throw new JsonQueryException("indices() is not applicable to " + haystackType);
		}
		return result;
	}
}
