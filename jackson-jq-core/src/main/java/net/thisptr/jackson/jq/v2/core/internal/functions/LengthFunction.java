package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.UnicodeUtils;
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
@FunctionRegistration("length/0")
public class LengthFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		output.emit(length(jsonProvider, in), null);
	}

	public <JsonNode> JsonNode length(JsonProvider<JsonNode> jsonProvider, JsonNode in) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
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
