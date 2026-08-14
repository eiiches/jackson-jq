package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
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
@FunctionRegistration(name = "tonumber", nargs = 0)
public class ToNumberFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.NUMBER) {
			output.emit(in, null);
		} else if (inType == JsonNodeType.STRING) {
			try {
				double value = Double.parseDouble(jsonProvider.asText(in));
				output.emit(JsonNodeUtils.asNumericNode(jsonProvider, value), null);
			} catch (NumberFormatException e) {
				throw new JsonQueryException(e);
			}
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s cannot be parsed as a number", in);
		}
	}
}
