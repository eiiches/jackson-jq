package net.thisptr.jackson.jq.internal.functions;

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
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction(value = "utf8bytelength/0", version = "[1.6, )")
public class Utf8ByteLengthFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (jsonProvider.getNodeType(in) != JsonNodeType.STRING)
			throw new JsonQueryTypeException(jsonProvider, "%s only strings have UTF-8 byte length", in);
		output.emit(jsonProvider.createNumber(UnicodeUtils.lengthUtf8(jsonProvider.asText(in))), null);
	}
}
