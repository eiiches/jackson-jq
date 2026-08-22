package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "reverse", nargs = 0)
public class ReverseFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {

			JsonNode out = jsonProvider.createArray();

			JsonNodeType type = jsonProvider.getNodeType(in);
			if (type == JsonNodeType.NULL) {
				output.emit(out, null);
				return;
			}
			if (type == JsonNodeType.ARRAY) {
				int size = jsonProvider.size(in);
				for (int i = size - 1; i >= 0; --i)
					jsonProvider.add(out, jsonProvider.requireGet(in, i));
				output.emit(out, null);
				return;
			}

			// below are to emulate jq behavior

			if (type == JsonNodeType.STRING) {
				if (jsonProvider.asText(in).isEmpty()) {
					output.emit(out, null);
					return;
				}
				throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(0)));
			}
			if (type == JsonNodeType.NUMBER) {
				if (jsonProvider.asDouble(in) == 0.0) {
					output.emit(out, null);
					return;
				}
				throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(0)));
			}
			if (type == JsonNodeType.OBJECT) {
				if (jsonProvider.size(in) == 0) {
					output.emit(out, null);
					return;
				}
				throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(0)));
			}
			if (type == JsonNodeType.BOOLEAN) {
				throw new JsonQueryTypeException(jsonProvider, version, "%s has no length", in);
			}
			throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be reversed", in);
		});
	}
}
