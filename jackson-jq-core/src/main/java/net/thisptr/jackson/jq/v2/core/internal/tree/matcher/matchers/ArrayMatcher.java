package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.path.ArrayIndexPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ArrayMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private List<PatternMatcher<JsonNode>> matchers;
	private final @Nullable Version version;

	public ArrayMatcher(JsonProvider<JsonNode> jsonProvider, List<PatternMatcher<JsonNode>> matchers) {
		this(jsonProvider, matchers, null);
	}

	public ArrayMatcher(JsonProvider<JsonNode> jsonProvider, List<PatternMatcher<JsonNode>> matchers, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.matchers = matchers;
		this.version = version;
	}

	public List<PatternMatcher<JsonNode>> matchers() {
		return matchers;
	}

	private void recursive(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY && jsonProvider.getNodeType(in) != JsonNodeType.NULL)
			throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = jsonProvider.get(in, rindex);

		matcher.match(frame, value != null ? value : jsonProvider.createNull(), (match) -> {
			recursive(frame, in, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void match(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot index %s with number", type);
		}
		recursive(frame, in, out, accumulate, 0);
	}

	private void recursiveWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> out, Deque<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.emit(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY && jsonProvider.getNodeType(in) != JsonNodeType.NULL)
			throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = jsonProvider.get(in, rindex);
		ArrayIndexPath<JsonNode> valuePath = ArrayIndexPath.chainIfNotNull(jsonProvider, path, rindex, version);

		matcher.matchWithPath(frame, value != null ? value : jsonProvider.createNull(), valuePath, (match) -> {
			recursiveWithPath(frame, in, path, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void matchWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> out, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot index %s with number", type);
		}
		recursiveWithPath(frame, in, path, out, accumulate, 0);
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
		List<PatternMatcher<JsonNode>> resolved = new ArrayList<>(matchers.size());
		for (PatternMatcher<JsonNode> matcher : matchers)
			resolved.add(matcher.resolveSlots(slots));
		return new ArrayMatcher<>(jsonProvider, resolved, version);
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
