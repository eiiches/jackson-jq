package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.commons.collection.Lists;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractKeysFunction implements Function {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(String name, boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

			if (jsonProvider.isObject(in)) {
				List<String> keys = Lists.newArrayList(jsonProvider.getObjectMemberNames(in));
				if (sortKeys)
					Collections.sort(keys);

				List<JsonNode> result = new ArrayList<>();
				for (String key : keys)
					result.add(jsonProvider.createString(key));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
			} else if (jsonProvider.isArray(in)) {
				List<JsonNode> result = new ArrayList<>();
				for (int i = 0; i < jsonProvider.getArrayLength(in); ++i)
					result.add(jsonProvider.createNumber(i));
				output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
			} else {
				throw new IllegalStateException();
			}
		});
	}
}
