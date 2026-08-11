package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BreakExpression<JsonNode> implements Expression<JsonNode> {
	private final String name;

	public BreakExpression(final String name) {
		this.name = name;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		throw new JsonQueryBreakException(name);
	}

	@Override
	public String toString() {
		return "break $" + name;
	}
}
