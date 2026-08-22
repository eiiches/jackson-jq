package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class UriDecodeFunction implements Function {
	@Override
	// Suppress JdkObsolete because URLDecoder.decode(String, Charset) is not available in Java 8 target.
	@SuppressWarnings("JdkObsolete")
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "urldecode", in, JsonNodeType.STRING);

			try {
				output.emit(jsonProvider.createString(URLDecoder.decode(jsonProvider.asText(in), StandardCharsets.UTF_8.name())), null);
			} catch (UnsupportedEncodingException e) {
				throw new JsonQueryException(e);
			}
		};
	}
}
