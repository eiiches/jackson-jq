package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public class ObjectMatcher implements PatternMatcher {
	private List<Pair<Expression, PatternMatcher>> matchers;

	public ObjectMatcher(final List<Pair<Expression, PatternMatcher>> matchers) {
		this.matchers = matchers;
	}

	private void recursive(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit, int index) throws JsonQueryException {
		if (index >= matchers.size())
			return;

		final Pair<Expression, PatternMatcher> kvexpr = matchers.get(index);
		final Expression keyexpr = kvexpr._1;
		final PatternMatcher matcher = kvexpr._2;

		keyexpr.apply(scope, in, (key) -> {
			if (!key.isTextual())
				throw new JsonQueryTypeException("Cannot index %s with %s", in.getNodeType(), key.getNodeType());

			final JsonNode value = in.get(key.asText());

			final int size = accumulate.size();
			matcher.match(scope, value != null ? value : NullNode.getInstance(), out, accumulate, emit && index == matchers.size() - 1);
			recursive(scope, in, out, accumulate, emit, index + 1);
			accumulate.setSize(size);
		});
	}

	private void recursiveWithPath(final Scope scope, final JsonNode in, final Path inpath, final MatchOutput output, final Stack<MatchWithPath> accumulate, final boolean emit, int index) throws JsonQueryException {
		if (index >= matchers.size())
			return;

		final Pair<Expression, PatternMatcher> kvexpr = matchers.get(index);
		final Expression keyexpr = kvexpr._1;
		final PatternMatcher matcher = kvexpr._2;

		keyexpr.apply(scope, in, (key) -> {
			if (!key.isTextual())
				throw new JsonQueryTypeException("Cannot index %s with %s", in.getNodeType(), key.getNodeType());

			final JsonNode value = in.get(key.asText());
			final Path valuepath = ObjectFieldPath.chainIfNotNull(inpath, key.asText());

			final int size = accumulate.size();
			matcher.matchWithPath(scope, value != null ? value : NullNode.getInstance(), valuepath, output, accumulate, emit && index == matchers.size() - 1);
			recursiveWithPath(scope, in, inpath, output, accumulate, emit, index + 1);
			accumulate.setSize(size);
		});
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException {
		if (!in.isObject() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with string", in.getNodeType());

		recursive(scope, in, out, accumulate, emit, 0);
	}

	@Override
	public void matchWithPath(Scope scope, JsonNode in, Path path, MatchOutput output, Stack<MatchWithPath> accumulate, boolean emit) throws JsonQueryException {
		if (!in.isObject() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with string", in.getNodeType());

		recursiveWithPath(scope, in, path, output, accumulate, emit, 0);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		String sep = "";
		for (final Pair<Expression, PatternMatcher> entry : matchers) {
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