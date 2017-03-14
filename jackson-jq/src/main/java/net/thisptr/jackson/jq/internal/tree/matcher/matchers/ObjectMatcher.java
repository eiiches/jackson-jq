package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ObjectMatcher implements PatternMatcher {
	private List<Pair<JsonQuery, PatternMatcher>> matchers;

	public ObjectMatcher(final List<Pair<JsonQuery, PatternMatcher>> matchers) {
		this.matchers = matchers;
	}

	private void recursive(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit, int index) throws JsonQueryException {
		if (index >= matchers.size())
			return;

		final Pair<JsonQuery, PatternMatcher> kvexpr = matchers.get(index);
		final JsonQuery keyexpr = kvexpr._1;
		final PatternMatcher matcher = kvexpr._2;

		for (final JsonNode key : keyexpr.apply(scope, in)) {
			if (!key.isTextual())
				throw JsonQueryTypeException.format("Cannot index %s with %s", in.getNodeType(), key.getNodeType());

			final JsonNode value = in.get(key.asText());

			final int size = accumulate.size();
			matcher.match(scope, value != null ? value : NullNode.getInstance(), out, accumulate, emit && index == matchers.size() - 1);
			recursive(scope, in, out, accumulate, emit, index + 1);
			accumulate.setSize(size);
		}
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException {
		if (!in.isObject() && !in.isNull())
			throw JsonQueryTypeException.format("Cannot index %s with string", in.getNodeType());

		recursive(scope, in, out, accumulate, emit, 0);
	}

	// . as [$a, {(.a, .a): $b}] === .[0] as $a | (.[1].a, .[1].a) as $b

	// public void match(final Scope scope, final JsonNode in, final boolean emit, final Map<String, JsonNode> accum, final Consumer<Map<String, JsonNode>> out)
	// throws JsonQueryException {
	// if (!in.isObject() && !in.isNull())
	// throw JsonQueryTypeException.format("Cannot index %s with string", in.getNodeType());
	//
	// recursive(scope, in, emit, out, 0);
	// }
	//
	// {
	// for (int i = 0; i < matchers.size(); ++i) {
	// final PatternMatcher matcher = matchers.get(i);
	//
	// for (final JsonNode key : entry.getKey().apply(scope, in)) {
	// if (!key.isTextual())
	// throw JsonQueryTypeException.format("Cannot index %s with %s", in.getNodeType(), key.getNodeType());
	//
	// final JsonNode value = in.get(key.asText());
	// matcher.match(value != null ? value : NullNode.getInstance(), out);
	// }
	// }
	// }

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		String sep = "";
		for (final Pair<JsonQuery, PatternMatcher> entry : matchers) {
			sb.append(sep);
			sb.append(entry._1);
			sb.append(": ");
			sb.append(entry._2);
			sep = ", ";
		}
		sb.append("}");
		return sb.toString();
	}
}