package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryUserException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "error", nargs = 0)
@FunctionRegistration(name = "error", nargs = 1)
public class ErrorFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
								if (args.isEmpty()) {
					if (jsonProvider.getNodeType(in) == JsonNodeType.NULL)
						return;
					throw new JsonQueryUserException(jsonProvider, in);
				} else {
					args.get(0).apply(frame, in, (out) -> {
						if (jsonProvider.getNodeType(out) == JsonNodeType.NULL)
							return;
						throw new JsonQueryUserException(jsonProvider, out);
					});
				}
	};
	}
}
