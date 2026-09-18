package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
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
	private final List<FieldMatcher<JsonNode>> matchers;
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

		/**
		 * Output counter for the key expression, which this matcher evaluates through a sink of its own --
		 * once per object matched, so it accumulates like any other re-evaluated expression. Carried through
		 * {@link #resolveSlots} the same way {@link #writeSlot} is.
		 */
		private final int nameOutputIndex;

		public FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher, int nameOutputIndex) {
			this(dollar, variableName, name, matcher, -1, nameOutputIndex);
		}

		private FieldMatcher(boolean dollar, @Nullable String variableName, Expression<StackFrame, JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher, int writeSlot, int nameOutputIndex) {
			if (dollar && variableName == null)
				throw new IllegalArgumentException("BUG: variableName must not be null when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.variableName = variableName;
			this.name = name;
			this.matcher = matcher;
			this.writeSlot = writeSlot;
			this.nameOutputIndex = nameOutputIndex;
		}

		private FieldMatcher<JsonNode> resolveSlots(SlotResolver resolver) {
			// The field's own binding is claimed before the sub-pattern's, because that is the order
			// recursive() writes them in.
			int resolvedSlot = resolveWriteSlot(resolver);
			return new FieldMatcher<>(dollar, variableName, name, matcher != null ? matcher.resolveSlots(resolver) : null, resolvedSlot, nameOutputIndex);
		}

		private FieldMatcher<JsonNode> rewriteExpressions(UnaryOperator<Expression<StackFrame, JsonNode>> rewriter) {
			Expression<StackFrame, JsonNode> rewrittenName = rewriter.apply(name);
			PatternMatcher<JsonNode> rewrittenMatcher = matcher != null ? matcher.rewriteExpressions(rewriter) : null;
			return rewrittenName == name && rewrittenMatcher == matcher
					? this
					: new FieldMatcher<>(dollar, variableName, rewrittenName, rewrittenMatcher, writeSlot, nameOutputIndex);
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
		Memory memory = frame.getEnclosingMemory();
		fmatcher.name.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			memory.countOutput(fmatcher.nameOutputIndex);
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
		Memory memory = frame.getEnclosingMemory();
		fmatcher.name.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			memory.countOutput(fmatcher.nameOutputIndex);
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

	@Override
	public PatternMatcher<JsonNode> rewriteExpressions(UnaryOperator<Expression<StackFrame, JsonNode>> rewriter) {
		@Var List<FieldMatcher<JsonNode>> rewritten = null;
		for (int i = 0; i < matchers.size(); i++) {
			FieldMatcher<JsonNode> matcher = matchers.get(i);
			FieldMatcher<JsonNode> replacement = matcher.rewriteExpressions(rewriter);
			if (rewritten == null && replacement != matcher)
				rewritten = new ArrayList<>(matchers);
			if (rewritten != null)
				rewritten.set(i, replacement);
		}
		return rewritten == null ? this : new ObjectMatcher<>(jsonProvider, rewritten, version);
	}
}
