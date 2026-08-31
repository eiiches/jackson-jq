package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractTrimStrFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (trimText, opath) -> {
				if (jsonProvider.getNodeType(in) != JsonNodeType.STRING || jsonProvider.getNodeType(trimText) != JsonNodeType.STRING) {
					output.emit(in, ipath);
					return;
				}
				JsonNode out = jsonProvider.createString(doTrim(jsonProvider.getString(in), jsonProvider.getString(trimText)));
				output.emit(out, UntrackedPath.getInstance());
			});
		});
	}

	protected abstract String doTrim(String text, String trim);
}
