package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.SlotResolver;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
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

		/**
		 * The frame slot the field's own {@code $}-binding writes, or -1 when there is none -- either
		 * because this is not a {@code $}-field, or because a previous occurrence of the same variable
		 * shadows it (see {@link SlotResolver#claim}).
		 */
		private final int writeSlot;

		public FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher) {
			this(dollar, variableName, name, matcher, -1);
		}

		private FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher, int writeSlot) {
			if (dollar && variableName == null)
				throw new IllegalArgumentException("BUG: variableName must not be null when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.variableName = variableName;
			this.name = name;
			this.matcher = matcher;
			this.writeSlot = writeSlot;
		}

		private FieldMatcher<JsonNode> resolveSlots(SlotResolver resolver) {
			// The field's own binding is claimed before the sub-pattern's, because that is the order
			// recursive() writes them in.
			int resolvedSlot = resolveWriteSlot(resolver);
			return new FieldMatcher<>(dollar, variableName, name, matcher != null ? matcher.resolveSlots(resolver) : null, resolvedSlot);
		}

		private int resolveWriteSlot(SlotResolver resolver) {
			if (!dollar)
				return -1;
			if (variableName == null)
				throw new IllegalStateException("BUG: variableName is null when dollar = true");
			return resolver.claim(variableName);
		}
	}

	private void recursive(StackFrame frame, JsonNode in, OnMatch onMatch, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			onMatch.matched();
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

			if (fmatcher.writeSlot >= 0)
				frame.set(fmatcher.writeSlot, StackFrameValues.toSlot(value));
			if (fmatcher.matcher != null) {
				fmatcher.matcher.match(frame, value, () -> recursive(frame, in, onMatch, index + 1));
			} else {
				recursive(frame, in, onMatch, index + 1);
			}
		});
	}

	private void recursiveWithPath(StackFrame frame, JsonNode in, Path<JsonNode> inpath, OnMatch onMatch, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			onMatch.matched();
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

			if (fmatcher.writeSlot >= 0)
				frame.set(fmatcher.writeSlot, StackFrameValues.toSlot(value, valuepath));
			if (fmatcher.matcher != null) {
				fmatcher.matcher.matchWithPath(frame, value, valuepath, () -> recursiveWithPath(frame, in, inpath, onMatch, index + 1));
			} else {
				recursiveWithPath(frame, in, inpath, onMatch, index + 1);
			}
		});
	}

	@Override
	public void match(StackFrame frame, JsonNode in, OnMatch onMatch) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with string", ExceptionMessages.typeName(type));
		}

		recursive(frame, in, onMatch, 0);
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL) {
			if (matchers.isEmpty())
				throw new JsonQueryTypeException("Cannot index %s with string", ExceptionMessages.typeName(type));
		}

		recursiveWithPath(frame, in, path, onMatch, 0);
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(SlotResolver resolver) {
		List<FieldMatcher<JsonNode>> resolved = new ArrayList<>(matchers.size());
		for (FieldMatcher<JsonNode> matcher : matchers)
			resolved.add(matcher.resolveSlots(resolver));
		return new ObjectMatcher<>(jsonProvider, resolved, version);
	}
}
