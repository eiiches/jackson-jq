package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ArrayMatcher implements PatternMatcher {
	private List<PatternMatcher> matchers;

	public ArrayMatcher(final List<PatternMatcher> matchers) {
		this.matchers = matchers;
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException {
		if (!in.isArray() && !in.isNull())
			throw JsonQueryTypeException.format("Cannot index %s with number", in.getNodeType());
		for (int i = 0; i < matchers.size(); ++i) {
			final PatternMatcher matcher = matchers.get(i);
			final JsonNode item = in.get(i);
			matcher.match(scope, item != null ? item : NullNode.getInstance(), out, accumulate, emit && i == matchers.size() - 1);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		String sep = "";
		for (final PatternMatcher matcher : matchers) {
			sb.append(sep);
			sb.append(matcher);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}