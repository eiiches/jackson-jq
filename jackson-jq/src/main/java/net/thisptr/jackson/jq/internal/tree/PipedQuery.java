package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class PipedQuery extends JsonQuery {
	private List<Pair<JsonQuery, PatternMatcher>> qs;

	public PipedQuery(final List<Pair<JsonQuery, PatternMatcher>> qs) {
		this.qs = qs;
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(scope, in, out, qs);
		return out;
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out, final List<Pair<JsonQuery, PatternMatcher>> qs) throws JsonQueryException {
		if (qs.isEmpty()) {
			out.add(in);
			return;
		}
		final Pair<JsonQuery, PatternMatcher> head = qs.get(0);
		final JsonQuery q = head._1;
		final PatternMatcher matcher = head._2;

		final Scope scope2 = matcher != null
				? Scope.newChildScope(scope)
				: scope;

		for (final JsonNode o : q.apply(scope, in)) {
			if (matcher != null) {
				final Stack<Pair<String, JsonNode>> accumulate = new Stack<>();
				matcher.match(scope, o, (final List<Pair<String, JsonNode>> vars) -> {
					// Set values in reverse order since if there is the variable name crash,
					// jq only uses the first match.
					for (int i = vars.size() - 1; i >= 0; --i) {
						final Pair<String, JsonNode> var = vars.get(i);
						scope2.setValue(var._1, var._2);
					}
					applyRecursive(scope2, in, out, qs.subList(1, qs.size()));
				}, accumulate, true);
			} else {
				applyRecursive(scope2, o, out, qs.subList(1, qs.size()));
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("(");
		String sep = "";
		for (final Pair<JsonQuery, PatternMatcher> q : qs) {
			builder.append(sep);
			builder.append(q._1.toString());
			if (q._2 != null) {
				builder.append(" as ");
				builder.append(q._2);
			}
			sep = " | ";
		}
		builder.append(")");
		return builder.toString();
	}
}
