package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "has", nargs = 1)
public class HasFunction implements FunctionFactory {
	@Override
	public <JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType == JsonNodeType.NULL) {
				output.emit(jsonProvider.createBoolean(false), null);
				return;
			}
			args.get(0).apply(frame, in, (keyName) -> {
				JsonNodeType keyType = jsonProvider.getNodeType(keyName);
				if (inType == JsonNodeType.OBJECT) {
					if (keyType != JsonNodeType.STRING)
						throw new JsonQueryException("argument 1 of has() must be string for object input");
					output.emit(jsonProvider.createBoolean(jsonProvider.has(in, jsonProvider.asText(keyName))), null);
				} else if (inType == JsonNodeType.ARRAY) {
					if (keyType != JsonNodeType.NUMBER)
						throw new JsonQueryException("argument 1 of has() must be int for array input");
					double keyAsDouble = jsonProvider.asDouble(keyName);
					if (Double.isNaN(keyAsDouble) || Double.isInfinite(keyAsDouble))
						throw new JsonQueryException("argument 1 of has() must be int for array input, got " + (Double.isNaN(keyAsDouble) ? "nan" : "infinite"));
					int keyAsInt = (int) keyAsDouble;
					if (keyAsDouble != keyAsInt)
						throw new JsonQueryException("argument 1 of has() must be int for array input, got " + keyAsDouble);
					output.emit(jsonProvider.createBoolean(jsonProvider.has(in, keyAsInt)), null);
				} else {
					throw new JsonQueryException("has() is not applicable to " + inType);
				}
			});
		};
	}
}
