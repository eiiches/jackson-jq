package net.thisptr.jackson.jq.v2.test.random;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class LiteralExpression implements Expression<JsonNode> {
	private final JsonNode value;

	public LiteralExpression(JsonNode value) {
		this.value = value;
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		output.emit(value, null);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
