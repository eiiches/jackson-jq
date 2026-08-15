package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryUserException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "error", nargs = 0)
@FunctionRegistration(name = "error", nargs = 1)
public class ErrorFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, ipath, output) -> {
								if (args.isEmpty()) {
					if (jsonProvider.getNodeType(in) == JsonNodeType.NULL)
						return;
					throw new JsonQueryUserException(jsonProvider, in);
				} else {
					args.get(0).apply(scope, in, (out) -> {
						if (jsonProvider.getNodeType(out) == JsonNodeType.NULL)
							return;
						throw new JsonQueryUserException(jsonProvider, out);
					});
				}
	};
	}
}
