package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.Path;

public class ArrayMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private List<PatternMatcher<JsonNode>> matchers;

	public ArrayMatcher(final List<PatternMatcher<JsonNode>> matchers) {
		this.matchers = matchers;
	}

	private void recursive(final Scope<JsonNode> scope, final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		final int rindex = matchers.size() - index - 1;
		final PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		final JsonNode value = jsonProvider.get(in, rindex);

		matcher.match(scope, value != null ? value : jsonProvider.createNull(), (match) -> {
			recursive(scope, jsonProvider, in, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void match(final Scope<JsonNode> scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", type);
		recursive(scope, jsonProvider, in, out, accumulate, 0);
	}

	private void recursiveWithPath(final Scope<JsonNode> scope, final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> path, final MatchOutput<JsonNode> out, final Stack<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.emit(accumulate);
			return;
		}

		final int rindex = matchers.size() - index - 1;
		final PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		final JsonNode value = jsonProvider.get(in, rindex);
		final ArrayIndexPath<JsonNode> valuePath = ArrayIndexPath.chainIfNotNull(jsonProvider, path, rindex);

		matcher.matchWithPath(scope, value != null ? value : jsonProvider.createNull(), valuePath, (match) -> {
			recursiveWithPath(scope, jsonProvider, in, path, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void matchWithPath(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final MatchOutput<JsonNode> out, final Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", type);
		recursiveWithPath(scope, jsonProvider, in, path, out, accumulate, 0);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("[");
		String sep = "";
		for (final PatternMatcher<JsonNode> matcher : matchers) {
			sb.append(sep);
			sb.append(matcher);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}
