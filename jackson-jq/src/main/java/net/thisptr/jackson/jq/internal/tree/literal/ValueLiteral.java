package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class ValueLiteral<JsonNode> implements Expression<JsonNode> {

	public abstract JsonNode value(JsonProvider<JsonNode> jsonProvider);

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		output.emit(value(scope.jsonProvider()), null);
	}
}
