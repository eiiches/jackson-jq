package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ArrayMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private List<PatternMatcher<JsonNode>> matchers;

	public ArrayMatcher(List<PatternMatcher<JsonNode>> matchers) {
		this.matchers = matchers;
	}

	public List<PatternMatcher<JsonNode>> matchers() {
		return matchers;
	}

	private void recursive(Scope<JsonNode> scope, JsonProvider<JsonNode> jsonProvider, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = jsonProvider.get(in, rindex);

		matcher.match(scope, value != null ? value : jsonProvider.createNull(), (match) -> {
			recursive(scope, jsonProvider, in, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void match(Scope<JsonNode> scope, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", type);
		recursive(scope, jsonProvider, in, out, accumulate, 0);
	}

	private void recursiveWithPath(Scope<JsonNode> scope, JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> out, Stack<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.emit(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = jsonProvider.get(in, rindex);
		ArrayIndexPath<JsonNode> valuePath = ArrayIndexPath.chainIfNotNull(jsonProvider, path, rindex);

		matcher.matchWithPath(scope, value != null ? value : jsonProvider.createNull(), valuePath, (match) -> {
			recursiveWithPath(scope, jsonProvider, in, path, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void matchWithPath(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> out, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with number", type);
		recursiveWithPath(scope, jsonProvider, in, path, out, accumulate, 0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		@Var String sep = "";
		for (PatternMatcher<JsonNode> matcher : matchers) {
			sb.append(sep);
			sb.append(matcher);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}
