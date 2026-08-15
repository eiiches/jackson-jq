package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class UriDecodeFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "urldecode", in, JsonNodeType.STRING);

			try {
				output.emit(jsonProvider.createString(URLDecoder.decode(jsonProvider.asText(in), "UTF-8")), null);
			} catch (UnsupportedEncodingException e) {
				throw new JsonQueryException(e);
			}
		};
	}
}
