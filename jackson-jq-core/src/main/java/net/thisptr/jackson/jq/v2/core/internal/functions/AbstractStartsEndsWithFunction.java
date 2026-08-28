package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class AbstractStartsEndsWithFunction implements Function {
	private final String fname;

	public AbstractStartsEndsWithFunction(String fname) {
		this.fname = fname;
	}

	protected abstract boolean doCheck(String text, String needle);

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> {
				if (jsonProvider.getNodeType(needle) != JsonNodeType.STRING || jsonProvider.getNodeType(in) != JsonNodeType.STRING)
					throw new JsonQueryException(fname + "() requires string inputs");
				output.emit(jsonProvider.createBoolean(doCheck(jsonProvider.asText(in), jsonProvider.asText(needle))), UntrackedPath.getInstance());
			});
		});
	}
}
