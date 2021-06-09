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
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.Path;

public class ArrayMatcher implements PatternMatcher {
	private List<PatternMatcher> matchers;

	public ArrayMatcher(final List<PatternMatcher> matchers) {
		this.matchers = matchers;
	}

	private void recursive(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		final PatternMatcher matcher = matchers.get(index);
		final JsonNode value = in.get(index);

		matcher.match(scope, value != null ? value : NullNode.getInstance(), (match) -> {
			recursive(scope, in, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		if (!in.isArray() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with number", in.getNodeType());
		recursive(scope, in, out, accumulate, 0);
	}

	private void recursiveWithPath(final Scope scope, final JsonNode in, final Path path, final MatchOutput out, final Stack<MatchWithPath> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.emit(accumulate);
			return;
		}

		final PatternMatcher matcher = matchers.get(index);
		final JsonNode value = in.get(index);
		final ArrayIndexPath valuePath = ArrayIndexPath.chainIfNotNull(path, index);

		matcher.matchWithPath(scope, value != null ? value : NullNode.getInstance(), valuePath, (match) -> {
			recursiveWithPath(scope, in, path, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void matchWithPath(final Scope scope, final JsonNode in, final Path path, final MatchOutput out, final Stack<MatchWithPath> accumulate) throws JsonQueryException {
		if (!in.isArray() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with number", in.getNodeType());
		recursiveWithPath(scope, in, path, out, accumulate, 0);
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