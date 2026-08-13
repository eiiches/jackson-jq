package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Tuple implements Expression {
	public final List<Expression> qs;

	public Tuple(List<Expression> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		for (Expression q : qs) {
			q.apply(scope, in, path, output, requirePath);
		}
	}
}
