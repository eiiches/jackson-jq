package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ObjectMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private List<FieldMatcher<JsonNode>> matchers;

	public ObjectMatcher(JsonProvider<JsonNode> jsonProvider, List<FieldMatcher<JsonNode>> matchers) {
		this.jsonProvider = jsonProvider;
		this.matchers = matchers;
	}

	public List<FieldMatcher<JsonNode>> matchers() {
		return matchers;
	}

	public static class FieldMatcher<JsonNode> {
		// e.g.
		// {$x} : dollar = true, name = "x", matcher = null
		// {$x: [$a]} : dollar = true, name = "x", matcher = [$a]
		// {x: [$a]} : dollar = false, name = "x", matcher = [$a]

		private boolean dollar;
		private Expression<JsonNode> name;
		private @Nullable PatternMatcher<JsonNode> matcher;
		private int slot;

		public FieldMatcher(boolean dollar, Expression<JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher) {
			this(dollar, name, matcher, -1);
		}

		private FieldMatcher(boolean dollar, Expression<JsonNode> name, @Nullable PatternMatcher<JsonNode> matcher, int slot) {
			if (dollar && !(name instanceof StringLiteral))
				throw new IllegalArgumentException("BUG: name must be instance of StringLiteral when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.name = name;
			this.matcher = matcher;
			this.slot = slot;
		}

		public boolean dollar() {
			return dollar;
		}

		public Expression<JsonNode> name() {
			return name;
		}

		public @Nullable PatternMatcher<JsonNode> rawMatcher() {
			return matcher;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (dollar) {
				sb.append("$");
				sb.append(((StringLiteral<JsonNode>) name).text());
			} else {
				sb.append(name);
			}
			if (matcher != null) {
				sb.append(": ");
				sb.append(matcher);
			}
			return sb.toString();
		}

		public PatternMatcher<JsonNode> matcher() {
			if (matcher == null)
				return new ValueMatcher<>(((StringLiteral<JsonNode>) name).text(), slot);
			return matcher;
		}

		private FieldMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
			@Var int resolvedSlot = slot;
			if (dollar) {
				String variableName = ((StringLiteral<JsonNode>) name).text();
				Integer value = slots.get(variableName);
				if (value == null)
					throw new IllegalStateException("No slot allocated for pattern variable $" + variableName);
				resolvedSlot = value.intValue();
			}
			return new FieldMatcher<>(dollar, name, matcher != null ? matcher.resolveSlots(slots) : null, resolvedSlot);
		}
	}

	private void recursive(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<List<Match<JsonNode>>> out, Stack<Match<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(frame, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));

			int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(new Match<>(fmatcher.slot, value != null ? value : jsonProvider.createNull()));
			fmatcher.matcher().match(frame, value != null ? value : jsonProvider.createNull(), (match) -> {
				recursive(frame, in, out, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	private void recursiveWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> inpath, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			output.emit(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(frame, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));
			@Nullable Path<JsonNode> valuepath = ObjectFieldPath.chainIfNotNull(inpath, jsonProvider.asText(key));

			int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(new MatchWithPath<>(fmatcher.slot, value != null ? value : jsonProvider.createNull(), valuepath));
			fmatcher.matcher().matchWithPath(frame, value != null ? value : jsonProvider.createNull(), valuepath, (match) -> {
				recursiveWithPath(frame, in, inpath, output, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	@Override
	public void match(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<List<Match<JsonNode>>> out, Stack<Match<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursive(frame, in, out, accumulate, 0);
	}

	@Override
	public void matchWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursiveWithPath(frame, in, path, output, accumulate, 0);
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
		List<FieldMatcher<JsonNode>> resolved = new ArrayList<>(matchers.size());
		for (FieldMatcher<JsonNode> matcher : matchers)
			resolved.add(matcher.resolveSlots(slots));
		return new ObjectMatcher<>(jsonProvider, resolved);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		@Var String sep = "";
		for (FieldMatcher<JsonNode> entry : matchers) {
			sb.append(sep);
			sb.append(entry.toString());
			sep = ", ";
		}
		sb.append("}");
		return sb.toString();
	}
}
