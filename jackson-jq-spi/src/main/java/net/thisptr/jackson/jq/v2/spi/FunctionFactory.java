package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

@FunctionalInterface
public interface FunctionFactory {
	<JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version);
}
