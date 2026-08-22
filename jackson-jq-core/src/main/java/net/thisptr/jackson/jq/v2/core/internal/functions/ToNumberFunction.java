package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "tonumber", nargs = 0)
public class ToNumberFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((scope, in, ipath, output) -> {

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
				throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be parsed as a number", in);
			}
		});
	}
}
