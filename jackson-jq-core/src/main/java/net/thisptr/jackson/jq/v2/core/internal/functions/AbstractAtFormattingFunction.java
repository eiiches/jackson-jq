package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractAtFormattingFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			String text = jsonProvider.getNodeType(in) == JsonNodeType.STRING
					? jsonProvider.asString(in)
					: JsonNodeUtils.toString(jsonProvider, in, version);
			output.emit(jsonProvider.createString(convert(text)), UntrackedPath.getInstance());
		});
	}

	public abstract String convert(String text) throws JsonQueryException;
}
