package net.thisptr.jackson.jq.internal.functions;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

// For security reasons, env/0 should not be loaded by default.
// @AutoService(Function.class)
// 2022-06-29(eiiches): commented out @BuiltinFunction("env/0") to make sure some custom function loaders don't load `env/0` accidentally.
// @BuiltinFunction("env/0")
public class EnvFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNode result = jsonProvider.createObject();
		for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
			jsonProvider.set(result, entry.getKey(), jsonProvider.createString(entry.getValue()));
		}
		output.emit(result, null);
	}
}
