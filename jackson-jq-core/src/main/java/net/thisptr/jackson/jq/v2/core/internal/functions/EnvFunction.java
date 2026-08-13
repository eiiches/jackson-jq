package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

// For security reasons, env/0 should not be loaded by default.
// @AutoService(Function.class)
// 2022-06-29(eiiches): commented out @FunctionRegistration("env/0") to make sure some custom function loaders don't load `env/0` accidentally.
// @FunctionRegistration("env/0")
public class EnvFunction implements Function {

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNode result = jsonProvider.createObject();
		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
			jsonProvider.set(result, entry.getKey(), jsonProvider.createString(entry.getValue()));
		}
		output.emit(result, null);
	}
}
