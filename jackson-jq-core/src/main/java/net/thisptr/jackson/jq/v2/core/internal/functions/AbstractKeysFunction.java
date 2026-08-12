package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Lists;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class AbstractKeysFunction implements Function {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(String name, boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

		if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
			List<String> keys = Lists.newArrayList(jsonProvider.fieldNames(in));
			if (sortKeys)
				Collections.sort(keys);

			JsonNode result = jsonProvider.createArray();
			for (String key : keys)
				jsonProvider.add(result, jsonProvider.createString(key));
			output.emit(result, null);
		} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
			JsonNode result = jsonProvider.createArray();
			for (int i = 0; i < jsonProvider.size(in); ++i)
				jsonProvider.add(result, jsonProvider.createInt(i));
			output.emit(result, null);
		} else {
			throw new IllegalStateException();
		}
	}
}
