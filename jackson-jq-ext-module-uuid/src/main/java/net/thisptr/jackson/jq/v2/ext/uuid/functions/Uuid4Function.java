package net.thisptr.jackson.jq.v2.ext.uuid.functions;

import java.util.List;
import java.util.UUID;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;

public class Uuid4Function implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			output.emit(jsonProvider.createString(UUID.randomUUID().toString()), null);
		};
	}
}
