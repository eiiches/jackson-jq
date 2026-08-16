package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.path.ObjectFieldPath;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ObjectMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private List<FieldMatcher<JsonNode>> matchers;

	public ObjectMatcher(List<FieldMatcher<JsonNode>> matchers) {
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
		private Expression name;
		private @Nullable PatternMatcher<JsonNode> matcher;

		public FieldMatcher(boolean dollar, Expression name, @Nullable PatternMatcher<JsonNode> matcher) {
			if (dollar && !(name instanceof StringLiteral))
				throw new IllegalArgumentException("BUG: name must be instance of StringLiteral when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.name = name;
			this.matcher = matcher;
		}

		public boolean dollar() {
			return dollar;
		}

		public Expression name() {
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
				sb.append(((StringLiteral) name).value());
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
				return new ValueMatcher<>(((StringLiteral) name).value());
			return matcher;
		}
	}

	private void recursive(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(jsonProvider, frame, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));

			int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(Pair.of(jsonProvider.asText(key), value != null ? value : jsonProvider.createNull()));
			fmatcher.matcher().match(jsonProvider, frame, value != null ? value : jsonProvider.createNull(), (match) -> {
				recursive(jsonProvider, frame, in, out, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	private void recursiveWithPath(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> inpath, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			output.emit(accumulate);
			return;
		}

		FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(jsonProvider, frame, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));
			@Nullable Path<JsonNode> valuepath = ObjectFieldPath.chainIfNotNull(inpath, jsonProvider.asText(key));

			int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(new MatchWithPath<>(jsonProvider.asText(key), value != null ? value : jsonProvider.createNull(), valuepath));
			fmatcher.matcher().matchWithPath(jsonProvider, frame, value != null ? value : jsonProvider.createNull(), valuepath, (match) -> {
				recursiveWithPath(jsonProvider, frame, in, inpath, output, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	@Override
	public void match(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursive(jsonProvider, frame, in, out, accumulate, 0);
	}

	@Override
	public void matchWithPath(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursiveWithPath(jsonProvider, frame, in, path, output, accumulate, 0);
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
