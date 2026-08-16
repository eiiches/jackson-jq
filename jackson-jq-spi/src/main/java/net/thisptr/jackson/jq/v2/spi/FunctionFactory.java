package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

@FunctionalInterface
public interface FunctionFactory {
	<JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version);

	default <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, @Nullable Closure<JsonNode> closure, List<Expression> args, Version version) {
		return createFunction(jsonProvider, args, version);
	}
}
