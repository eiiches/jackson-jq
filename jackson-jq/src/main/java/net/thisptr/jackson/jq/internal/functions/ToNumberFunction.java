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
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("tonumber/0")
public class ToNumberFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.NUMBER) {
			output.emit(in, null);
		} else if (inType == JsonNodeType.STRING) {
			try {
				final double value = Double.parseDouble(jsonProvider.asText(in));
				output.emit(JsonNodeUtils.asNumericNode(jsonProvider, value), null);
			} catch (final NumberFormatException e) {
				throw new JsonQueryException(e);
			}
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s cannot be parsed as a number", in);
		}
	}
}
