package net.thisptr.jackson.jq.v2.test.random;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class LiteralExpression implements Expression {
	private final JsonNode value;

	public LiteralExpression(JsonNode value) {
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> void apply(Scope<N> scope, N in, @Nullable Path<N> ipath, PathOutput<N> output, boolean requirePath) throws JsonQueryException {
		output.emit((N) value, null);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
