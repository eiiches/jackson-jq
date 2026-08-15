package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.List;
import java.util.Map.Entry;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;

public class DebugScopeFunction implements FunctionFactory {

	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, ipath, output) -> {
			@Var JsonNode functions = jsonProvider.createObject();
			for (Entry<String, FunctionFactory> f : scope.getLocalFunctionFactories().entrySet())
				functions = jsonProvider.set(functions, f.getKey(), jsonProvider.createString(f.getValue().toString()));

			@Var JsonNode scopeNode = jsonProvider.createObject();
			scopeNode = jsonProvider.set(scopeNode, "functions", functions);

			@Var JsonNode info = jsonProvider.createObject();
			info = jsonProvider.set(info, "scope", scopeNode);
			info = jsonProvider.set(info, "input", in);
			output.emit(info, null);
		};
	}
}
