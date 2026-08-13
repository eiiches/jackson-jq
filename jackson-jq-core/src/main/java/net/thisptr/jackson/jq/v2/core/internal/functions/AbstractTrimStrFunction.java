package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class AbstractTrimStrFunction implements Function {

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (trimText) -> {
			if (jsonProvider.getNodeType(in) != JsonNodeType.STRING || jsonProvider.getNodeType(trimText) != JsonNodeType.STRING) {
				output.emit(in, ipath);
				return;
			}
			JsonNode out = jsonProvider.createString(doTrim(jsonProvider.asText(in), jsonProvider.asText(trimText)));
			output.emit(out, null);
		});
	}

	protected abstract String doTrim(String text, String trim);
}
