package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.List;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class DebugScopeFunction implements Function {

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();

		JsonNode functions = jsonProvider.createObject();
		for (Entry<String, Function> f : scope.getLocalFunctions().entrySet())
			jsonProvider.set(functions, f.getKey(), jsonProvider.createString(f.getValue().toString()));

		JsonNode scopeNode = jsonProvider.createObject();
		jsonProvider.set(scopeNode, "functions", functions);

		JsonNode info = jsonProvider.createObject();
		jsonProvider.set(info, "scope", scopeNode);
		jsonProvider.set(info, "input", in);
		output.emit(info, null);
	}
}
