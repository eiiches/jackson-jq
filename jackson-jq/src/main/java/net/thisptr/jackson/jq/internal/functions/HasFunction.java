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
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("has/1")
public class HasFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.NULL) {
			output.emit(jsonProvider.createBoolean(false), null);
			return;
		}
		args.get(0).apply(scope, in, (keyName) -> {
			final JsonNodeType keyType = jsonProvider.getNodeType(keyName);
			if (inType == JsonNodeType.OBJECT) {
				if (keyType != JsonNodeType.STRING)
					throw new JsonQueryException("argument 1 of has() must be string for object input");
				output.emit(jsonProvider.createBoolean(jsonProvider.has(in, jsonProvider.asText(keyName))), null);
			} else if (inType == JsonNodeType.ARRAY) {
				if (keyType != JsonNodeType.NUMBER)
					throw new JsonQueryException("argument 1 of has() must be int for array input");
				final double keyAsDouble = jsonProvider.asDouble(keyName);
				if (Double.isNaN(keyAsDouble) || Double.isInfinite(keyAsDouble))
					throw new JsonQueryException("argument 1 of has() must be int for array input, got " + (Double.isNaN(keyAsDouble) ? "nan" : "infinite"));
				final int keyAsInt = (int) keyAsDouble;
				if (keyAsDouble != keyAsInt)
					throw new JsonQueryException("argument 1 of has() must be int for array input, got " + keyAsDouble);
				output.emit(jsonProvider.createBoolean(jsonProvider.has(in, keyAsInt)), null);
			} else {
				throw new JsonQueryException("has() is not applicable to " + inType);
			}
		});
	}
}
