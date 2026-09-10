package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ArrayMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private List<PatternMatcher<JsonNode>> matchers;
	private final Version version;

	public ArrayMatcher(JsonProvider<JsonNode> jsonProvider, List<PatternMatcher<JsonNode>> matchers, Version version) {
		this.jsonProvider = jsonProvider;
		this.matchers = matchers;
		this.version = version;
	}

	private JsonNode getArrayElementOrNull(JsonNode node, int index) {
		if (!jsonProvider.isArray(node) || index < 0 || index >= jsonProvider.getArrayLength(node))
			return jsonProvider.createNull();
		return jsonProvider.getArrayElement(node, index);
	}

	private void recursive(StackFrame frame, JsonNode in, Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (!jsonProvider.isArray(in) && !jsonProvider.isNull(in))
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = getArrayElementOrNull(in, rindex);

		matcher.match(frame, value, (match) -> {
			recursive(frame, in, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void match(StackFrame frame, JsonNode in, Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with number", ExceptionMessages.typeName(type));
		}
		recursive(frame, in, out, accumulate, 0);
	}

	private void recursiveWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> out, Deque<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.emit(accumulate);
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (!jsonProvider.isArray(in) && !jsonProvider.isNull(in))
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = getArrayElementOrNull(in, rindex);
		Path<JsonNode> valuePath = path.appendIndex(rindex);

		matcher.matchWithPath(frame, value, valuePath, (match) -> {
			recursiveWithPath(frame, in, path, out, accumulate, index + 1);
		}, accumulate);
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> out, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with number", ExceptionMessages.typeName(type));
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
}
