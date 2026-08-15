package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Expression {

	default <JsonNode> @Nullable JsonNode evaluateConstantExpr(JsonProvider<JsonNode> jsonProvider) {
		return null;
	}

	default <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(scope, in, null, output, false);
	}

	<JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException;
}
