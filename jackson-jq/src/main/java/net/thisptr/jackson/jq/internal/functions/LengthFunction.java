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
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("length/0")
public class LengthFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		output.emit(length(jsonProvider, in), null);
	}

	public JsonNode length(final JsonProvider<JsonNode> jsonProvider, final JsonNode in) throws JsonQueryException {
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.STRING) {
			return jsonProvider.createInt(UnicodeUtils.lengthUtf32(jsonProvider.asText(in)));
		} else if (type == JsonNodeType.ARRAY || type == JsonNodeType.OBJECT) {
			return jsonProvider.createInt(jsonProvider.size(in));
		} else if (type == JsonNodeType.NULL) {
			return jsonProvider.createInt(0);
		} else if (type == JsonNodeType.NUMBER) {
			return JsonNodeUtils.asNumericNode(jsonProvider, Math.abs(jsonProvider.asDouble(in)));
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s has no length", in);
		}
	}
}
