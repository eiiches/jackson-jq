package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class Tuple<JsonNode> implements Expression<JsonNode> {
	public final List<Expression<JsonNode>> qs;

	public Tuple(final List<Expression<JsonNode>> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		for (final Expression<JsonNode> q : qs) {
			q.apply(scope, in, path, output, requirePath);
		}
	}
}
