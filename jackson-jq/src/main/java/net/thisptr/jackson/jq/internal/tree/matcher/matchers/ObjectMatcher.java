package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public class ObjectMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private List<FieldMatcher<JsonNode>> matchers;

	public ObjectMatcher(final List<FieldMatcher<JsonNode>> matchers) {
		this.matchers = matchers;
	}

	public static class FieldMatcher<JsonNode> {
		// e.g.
		// {$x} : dollar = true, name = "x", matcher = null
		// {$x: [$a]} : dollar = true, name = "x", matcher = [$a]
		// {x: [$a]} : dollar = false, name = "x", matcher = [$a]

		private final boolean dollar;
		private final Expression<JsonNode> name;
		private final PatternMatcher<JsonNode> matcher;

		public FieldMatcher(final boolean dollar, final Expression<JsonNode> name, final PatternMatcher<JsonNode> matcher) {
			if (dollar && !(name instanceof StringLiteral))
				throw new IllegalArgumentException("BUG: name must be instance of StringLiteral when dollar = true");
			if (!dollar && matcher == null)
				throw new IllegalArgumentException("BUG: matcher must not be null when dollar = false");
			this.dollar = dollar;
			this.name = name;
			this.matcher = matcher;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if (dollar) {
				sb.append("$");
				sb.append(((StringLiteral<JsonNode>) name).value());
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
				return new ValueMatcher<>(((StringLiteral<JsonNode>) name).value());
			return matcher;
		}
	}

	private void recursive(final Scope<JsonNode> scope, final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		final FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(scope, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			final JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));

			final int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(Pair.of(jsonProvider.asText(key), value));
			fmatcher.matcher().match(scope, value != null ? value : jsonProvider.createNull(), (match) -> {
				recursive(scope, jsonProvider, in, out, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	private void recursiveWithPath(final Scope<JsonNode> scope, final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> inpath, final MatchOutput<JsonNode> output, final Stack<MatchWithPath<JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			output.emit(accumulate);
			return;
		}

		final FieldMatcher<JsonNode> fmatcher = matchers.get(index);
		fmatcher.name.apply(scope, in, (key) -> {
			if (jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(in), jsonProvider.getNodeType(key));

			final JsonNode value = jsonProvider.get(in, jsonProvider.asText(key));
			final Path<JsonNode> valuepath = ObjectFieldPath.chainIfNotNull(inpath, jsonProvider.asText(key));

			final int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(new MatchWithPath<>(jsonProvider.asText(key), value, valuepath));
			fmatcher.matcher().matchWithPath(scope, value != null ? value : jsonProvider.createNull(), valuepath, (match) -> {
				recursiveWithPath(scope, jsonProvider, in, inpath, output, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	@Override
	public void match(final Scope<JsonNode> scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursive(scope, jsonProvider, in, out, accumulate, 0);
	}

	@Override
	public void matchWithPath(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type != JsonNodeType.OBJECT && type != JsonNodeType.NULL)
			throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string", type);

		recursiveWithPath(scope, jsonProvider, in, path, output, accumulate, 0);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		String sep = "";
		for (final FieldMatcher<JsonNode> entry : matchers) {
			sb.append(sep);
			sb.append(entry.toString());
			sep = ", ";
		}
		sb.append("}");
		return sb.toString();
	}
}
