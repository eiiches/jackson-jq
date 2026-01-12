package net.thisptr.jackson.jq.internal.tree;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class SemicolonOperator<JsonNode> implements Expression<JsonNode> {
	private List<Expression<JsonNode>> qs;

	public SemicolonOperator(final List<Expression<JsonNode>> qs) {
		this.qs = qs;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		if (qs.isEmpty())
			return;
		for (final Expression<JsonNode> q : qs.subList(0, qs.size() - 1))
			q.apply(scope, in, (out) -> {});
		qs.get(qs.size() - 1).apply(scope, in, path, output, requirePath);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		String sep = "";
		for (final Expression<JsonNode> q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
