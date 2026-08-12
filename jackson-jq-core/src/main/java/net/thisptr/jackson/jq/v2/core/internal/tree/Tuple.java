package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Tuple<JsonNode> implements Expression<JsonNode> {
	public final List<Expression<JsonNode>> qs;

	public Tuple(List<Expression<JsonNode>> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		for (Expression<JsonNode> q : qs) {
			q.apply(scope, in, path, output, requirePath);
		}
	}
}
