package net.thisptr.jackson.jq.v2.spi;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface LegacyFunction extends Function {
	@Override
	@SuppressWarnings("unchecked")
	default <JsonNode> void apply(JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		apply(Scope.newEmptyScope((net.thisptr.jackson.jq.v2.json.JsonProvider<JsonNode>) null), Collections.emptyList(), in, path, output, Version.valueOf("1.7"));
	}

	<JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException;
}
