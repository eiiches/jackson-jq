package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public abstract class AbstractStartsEndsWithFunction implements FunctionFactory {
	private final String fname;

	public AbstractStartsEndsWithFunction(String fname) {
		this.fname = fname;
	}

	protected abstract boolean doCheck(String text, String needle);

	@Override
	public <JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			args.get(0).apply(frame, in, (needle) -> {
				if (jsonProvider.getNodeType(needle) != JsonNodeType.STRING || jsonProvider.getNodeType(in) != JsonNodeType.STRING)
					throw new JsonQueryException(fname + "() requires string inputs");
				output.emit(jsonProvider.createBoolean(doCheck(jsonProvider.asText(in), jsonProvider.asText(needle))), null);
			});
		};
	}
}
