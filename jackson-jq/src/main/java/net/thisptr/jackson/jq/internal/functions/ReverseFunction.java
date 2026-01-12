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
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("reverse/0")
public class ReverseFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNode out = jsonProvider.createArray();

		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.NULL) {
			output.emit(out, null);
			return;
		}
		if (type == JsonNodeType.ARRAY) {
			final int size = jsonProvider.size(in);
			for (int i = size - 1; i >= 0; --i)
				jsonProvider.add(out, jsonProvider.get(in, i));
			output.emit(out, null);
			return;
		}

		// below are to emulate jq behavior

		if (type == JsonNodeType.STRING) {
			if (jsonProvider.asText(in).isEmpty()) {
				output.emit(out, null);
				return;
			}
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", in);
		}
		if (type == JsonNodeType.NUMBER) {
			if (jsonProvider.asDouble(in) == 0.0) {
				output.emit(out, null);
				return;
			}
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", in);
		}
		if (type == JsonNodeType.OBJECT) {
			if (jsonProvider.size(in) == 0) {
				output.emit(out, null);
				return;
			}
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", in);
		}
		if (type == JsonNodeType.BOOLEAN) {
			throw new JsonQueryTypeException(jsonProvider, "%s has no length", in);
		}
		throw new JsonQueryTypeException(jsonProvider, "%s cannot be reversed", in);
	}
}
