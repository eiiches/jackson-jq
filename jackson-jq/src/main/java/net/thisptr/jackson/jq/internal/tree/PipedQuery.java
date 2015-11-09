package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;

import com.fasterxml.jackson.databind.JsonNode;

public class PipedQuery extends JsonQuery {
	private List<Pair<JsonQuery, String>> qs;

	public PipedQuery(final List<Pair<JsonQuery, String>> qs) {
		this.qs = qs;
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(scope, in, out, qs);
		return out;
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out, final List<Pair<JsonQuery, String>> qs) throws JsonQueryException {
		if (qs.isEmpty()) {
			out.add(in);
			return;
		}
		final Pair<JsonQuery, String> head = qs.get(0);
		final JsonQuery q = head._1;
		final String var = head._2;
		final Scope scope2 = var != null
				? new Scope(scope)
				: scope;
		for (final JsonNode o : q.apply(scope, in)) {
			if (var != null) {
				scope2.setValue(var, o);
				applyRecursive(scope2, in, out, qs.subList(1, qs.size()));
			} else {
				applyRecursive(scope2, o, out, qs.subList(1, qs.size()));
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("(");
		String sep = "";
		for (final Pair<JsonQuery, String> q : qs) {
			builder.append(sep);
			builder.append(q._1.toString());
			if (q._2 != null) {
				builder.append(" as $");
				builder.append(q._2);
			}
			sep = " | ";
		}
		builder.append(")");
		return builder.toString();
	}
}
