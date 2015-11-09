package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class SemicolonOperator extends JsonQuery {
	private List<JsonQuery> qs;

	public SemicolonOperator(final List<JsonQuery> qs) {
		this.qs = qs;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		List<JsonNode> result = Collections.emptyList();
		for (final JsonQuery q : qs)
			result = q.apply(scope, in);
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		String sep = "";
		for (final JsonQuery q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
