package net.thisptr.jackson.jq.extra.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@SuppressWarnings("rawtypes")
@AutoService(Function.class)
@BuiltinFunction("uridecode/0")
public class UriDecodeFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "urldecode", in, JsonNodeType.STRING);

		try {
			output.emit(jsonProvider.createString(URLDecoder.decode(jsonProvider.asText(in), "UTF-8")), null);
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
