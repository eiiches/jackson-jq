package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.LegacyFunction;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class UriDecodeFunction implements LegacyFunction {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "urldecode", in, JsonNodeType.STRING);

		try {
			output.emit(jsonProvider.createString(URLDecoder.decode(jsonProvider.asText(in), "UTF-8")), null);
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
