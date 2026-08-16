package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;

// For security reasons, env/0 should not be loaded by default.
// @AutoService(FunctionFactory.class)
// 2022-06-29(eiiches): commented out @FunctionRegistration(name = "env", nargs = 0) to make sure some custom function loaders don't load `env/0` accidentally.
// @FunctionRegistration(name = "env", nargs = 0)
public class EnvFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output) -> {
			JsonNode result = jsonProvider.createObject();
			for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
				jsonProvider.set(result, entry.getKey(), jsonProvider.createString(entry.getValue()));
			}
			output.emit(result, null);
		};
	}
}
