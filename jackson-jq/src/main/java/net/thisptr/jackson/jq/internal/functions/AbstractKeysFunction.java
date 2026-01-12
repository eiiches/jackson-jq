package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Lists;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public class AbstractKeysFunction<JsonNode> implements Function<JsonNode> {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(final String name, final boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			final List<String> keys = Lists.newArrayList(jsonProvider.fieldNames(in));
			if (sortKeys)
				Collections.sort(keys);

			final JsonNode result = jsonProvider.createArray();
			for (final String key : keys)
				jsonProvider.add(result, jsonProvider.createString(key));
			output.emit(result, null);
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			final JsonNode result = jsonProvider.createArray();
			for (int i = 0; i < jsonProvider.size(in); ++i)
				jsonProvider.add(result, jsonProvider.createInt(i));
			output.emit(result, null);
		} else {
			throw new IllegalStateException();
		}
	}
}
