package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.Lists;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class AbstractKeysFunction implements Function {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(String name, boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

			if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
				List<String> keys = Lists.newArrayList(jsonProvider.getObjectFieldNames(in));
				if (sortKeys)
					Collections.sort(keys);

				List<JsonNode> result = new ArrayList<>();
				for (String key : keys)
					result.add(jsonProvider.createString(key));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
			} else if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
				List<JsonNode> result = new ArrayList<>();
				for (int i = 0; i < jsonProvider.size(in); ++i)
					result.add(jsonProvider.createNumber(i));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
			} else {
				throw new IllegalStateException();
			}
		});
	}
}
