package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("reverse/0")
public class ReverseFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNode out = jsonProvider.createArray();

		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.NULL) {
			output.emit(out, null);
			return;
		}
		if (type == JsonNodeType.ARRAY) {
			int size = jsonProvider.size(in);
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
