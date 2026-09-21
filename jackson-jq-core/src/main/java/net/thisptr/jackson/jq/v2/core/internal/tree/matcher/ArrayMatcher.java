package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ArrayMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final List<PatternMatcher<JsonNode>> matchers;
	private final Version version;

	public ArrayMatcher(JsonProvider<JsonNode> jsonProvider, List<PatternMatcher<JsonNode>> matchers, Version version) {
		this.jsonProvider = jsonProvider;
		this.matchers = matchers;
		this.version = version;
	}

	public List<PatternMatcher<JsonNode>> matchers() {
		return matchers;
	}

	private JsonNode getArrayElementOrNull(JsonNode node, int index) {
		if (!jsonProvider.isArray(node) || index < 0 || index >= jsonProvider.getArrayLength(node))
			return jsonProvider.createNull();
		return jsonProvider.getArrayElement(node, index);
	}

	private void recursive(StackFrame frame, JsonNode in, OnMatch onMatch, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			onMatch.matched();
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (!jsonProvider.isArray(in) && !jsonProvider.isNull(in))
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = getArrayElementOrNull(in, rindex);

		matcher.match(frame, value, () -> recursive(frame, in, onMatch, index + 1));
	}

	@Override
	public <R> R accept(Visitor<JsonNode, R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void match(StackFrame frame, JsonNode in, OnMatch onMatch) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with number", ExceptionMessages.typeName(type));
		}
		recursive(frame, in, onMatch, 0);
	}

	private void recursiveWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			onMatch.matched();
			return;
		}

		int rindex = matchers.size() - index - 1;
		if (!jsonProvider.isArray(in) && !jsonProvider.isNull(in))
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, jsonProvider.createNumber(rindex)));

		PatternMatcher<JsonNode> matcher = matchers.get(rindex);
		JsonNode value = getArrayElementOrNull(in, rindex);
		Path<JsonNode> valuePath = path.appendIndex(rindex);

		matcher.matchWithPath(frame, value, valuePath, () -> recursiveWithPath(frame, in, path, onMatch, index + 1));
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.ARRAY && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with number", ExceptionMessages.typeName(type));
		}
		recursiveWithPath(frame, in, path, onMatch, 0);
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(SlotResolver resolver) {
		// Back to front, because that is the order recursive() traverses the elements in, and the
		// resolver decides duplicate-variable precedence from the order it is called in.
		List<PatternMatcher<JsonNode>> resolved = new ArrayList<>(matchers.size());
		for (int i = matchers.size() - 1; i >= 0; --i)
			resolved.add(matchers.get(i).resolveSlots(resolver));
		Collections.reverse(resolved);
		return new ArrayMatcher<>(jsonProvider, resolved, version);
	}

	@Override
	public PatternMatcher<JsonNode> rewriteExpressions(UnaryOperator<AnalyzedExpression<JsonNode>> rewriter) {
		@Var List<PatternMatcher<JsonNode>> rewritten = null;
		for (int i = 0; i < matchers.size(); i++) {
			PatternMatcher<JsonNode> matcher = matchers.get(i);
			PatternMatcher<JsonNode> replacement = matcher.rewriteExpressions(rewriter);
			if (rewritten == null && replacement != matcher)
				rewritten = new ArrayList<>(matchers);
			if (rewritten != null)
				rewritten.set(i, replacement);
		}
		return rewritten == null ? this : new ArrayMatcher<>(jsonProvider, rewritten, version);
	}
}
