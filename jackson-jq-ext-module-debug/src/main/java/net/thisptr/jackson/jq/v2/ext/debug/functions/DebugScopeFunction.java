package net.thisptr.jackson.jq.v2.ext.debug.functions;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;

// TODO: make this useful or remove
public class DebugScopeFunction implements Function {

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			JsonNode functions = jsonProvider.createObject();

			@Var JsonNode scopeNode = jsonProvider.createObject();
			scopeNode = jsonProvider.set(scopeNode, "functions", functions);

			@Var JsonNode info = jsonProvider.createObject();
			info = jsonProvider.set(info, "scope", scopeNode);
			info = jsonProvider.set(info, "input", in);
			output.emit(info, null);
		};
	}
}
