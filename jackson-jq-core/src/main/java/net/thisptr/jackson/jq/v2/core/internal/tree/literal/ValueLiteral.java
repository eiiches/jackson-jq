package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class ValueLiteral<JsonNode> implements Expression<JsonNode> {

	public abstract JsonNode value(JsonProvider<JsonNode> jsonProvider);

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		output.emit(value(scope.jsonProvider()), null);
	}
}
