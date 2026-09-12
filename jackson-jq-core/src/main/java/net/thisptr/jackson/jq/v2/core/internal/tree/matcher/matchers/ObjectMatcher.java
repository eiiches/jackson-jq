package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ObjectMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private List<FieldMatcher<JsonNode>> matchers;
	private final Version version;

	public ObjectMatcher(JsonProvider<JsonNode> jsonProvider, List<FieldMatcher<JsonNode>> matchers, Version version) {
		this.jsonProvider = jsonProvider;
		this.matchers = matchers;
		this.version = version;
	}

	public static class FieldMatcher<JsonNode> {
		// e.g.
		// {$x} : dollar = true, name = "x", matcher = null
		// {$x: [$a]} : dollar = true, name = "x", matcher = [$a]
		// {x: [$a]} : dollar = false, name = "x", matcher = [$a]

		private final boolean dollar;
		private final @Nullable String variableName;
		private final Expression<StackFrame, JsonNode> name;
		private final @Nullable PatternMatcher<JsonNode> matcher;
		private final int slot;

		public FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher) {
			this(dollar, variableName, name, matcher, -1);
		}

		private FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher, int slot) {
			if (dollar && variableName == null)
				throw new IllegalArgumentException("BUG: variableName must not be null when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.variableName = variableName;
			this.name = name;
			this.matcher = matcher;
			this.slot = slot;
		}

		public PatternMatcher<JsonNode> matcher() {
			if (matcher == null) {
				if (variableName == null)
					throw new IllegalStateException("BUG: variableName is null when matcher is null");
				return new ValueMatcher<>(variableName, slot);
			}
			return matcher;
		}

		private FieldMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
			@Var int resolvedSlot = slot;
			if (dollar) {
				if (variableName == null)
					throw new IllegalStateException("BUG: variableName is null when dollar = true");
				Integer value = slots.get(variableName);
				if (value == null)
					throw new IllegalStateException("No practical slot allocated for pattern variable $" + variableName);
				resolvedSlot = value.intValue();
			}
			return new FieldMatcher<>(dollar, variableName, name, matcher != null ? matcher.resolveSlots(slots) : null, resolvedSlot);
		}
	}

	private void recursive(StackFrame frame, JsonNode in, Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			if (!jsonProvider.isString(key))
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, key));
			if (!jsonProvider.isObject(in) && !jsonProvider.isNull(in))
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, key));

			JsonNode nullNode = jsonProvider.createNull();
			JsonNode value = jsonProvider.isObject(in)
					? jsonProvider.getObjectMemberOrDefault(in, jsonProvider.getString(key), nullNode)
					: nullNode;

			if (fmatcher.dollar)
				accumulate.addLast(new Match<>(fmatcher.slot, value));
			fmatcher.matcher().match(frame, value, (match) -> {
				recursive(frame, in, out, accumulate, index + 1);
			}, accumulate);
			if (fmatcher.dollar)
				accumulate.removeLast();
		});
	}

	private void recursiveWithPath(StackFrame frame, JsonNode in, Path<JsonNode> inpath, MatchOutput<JsonNode> output, Deque<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			output.emit(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			if (!jsonProvider.isString(key))
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, key));
			if (!jsonProvider.isObject(in) && !jsonProvider.isNull(in))
				throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, in, key));

			JsonNode nullNode = jsonProvider.createNull();
			JsonNode value = jsonProvider.isObject(in)
					? jsonProvider.getObjectMemberOrDefault(in, jsonProvider.getString(key), nullNode)
					: nullNode;
			Path<JsonNode> valuepath = inpath.appendKey(jsonProvider.getString(key));

			if (fmatcher.dollar)
				accumulate.addLast(new MatchWithPath<>(fmatcher.slot, value, valuepath));
			fmatcher.matcher().matchWithPath(frame, value, valuepath, (match) -> {
				recursiveWithPath(frame, in, inpath, output, accumulate, index + 1);
			}, accumulate);
			if (fmatcher.dollar)
				accumulate.removeLast();
		});
	}

	@Override
	public void match(StackFrame frame, JsonNode in, Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with string", ExceptionMessages.typeName(type));
		}

		recursive(frame, in, out, accumulate, 0);
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with string", ExceptionMessages.typeName(type));
		}

		recursiveWithPath(frame, in, path, output, accumulate, 0);
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
		List<FieldMatcher<JsonNode>> resolved = new ArrayList<>(matchers.size());
		for (FieldMatcher<JsonNode> matcher : matchers)
			resolved.add(matcher.resolveSlots(slots));
		return new ObjectMatcher<>(jsonProvider, resolved, version);
	}
}
