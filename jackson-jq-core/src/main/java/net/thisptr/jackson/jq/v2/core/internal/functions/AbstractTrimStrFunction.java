package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;

public abstract class AbstractTrimStrFunction implements Function {

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, null, (trimText, opath) -> {
				if (jsonProvider.getNodeType(in) != JsonNodeType.STRING || jsonProvider.getNodeType(trimText) != JsonNodeType.STRING) {
					output.emit(in, ipath);
					return;
				}
				JsonNode out = jsonProvider.createString(doTrim(jsonProvider.asText(in), jsonProvider.asText(trimText)));
				output.emit(out, null);
			});
		};
	}

	protected abstract String doTrim(String text, String trim);
}
