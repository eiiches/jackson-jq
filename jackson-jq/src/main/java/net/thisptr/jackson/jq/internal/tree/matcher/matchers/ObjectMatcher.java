package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.literal.StringLiteral;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.ObjectFieldPath;
import net.thisptr.jackson.jq.path.Path;

public class ObjectMatcher implements PatternMatcher {
	private List<FieldMatcher> matchers;

	public ObjectMatcher(final List<FieldMatcher> matchers) {
		this.matchers = matchers;
	}

	public ObjectMatcher() {}

	public List<FieldMatcher> getMatchers() {
		return Collections.unmodifiableList(matchers);
	}

	public void setMatchers(List<FieldMatcher> matchers) {
		this.matchers = matchers;
	}

	public static class FieldMatcher {
		// e.g.
		// {$x} : dollar = true, name = "x", matcher = null
		// {$x: [$a]} : dollar = true, name = "x", matcher = [$a]
		// {x: [$a]} : dollar = false, name = "x", matcher = [$a]

		private final boolean dollar;
		private final Expression name;
		private final PatternMatcher matcher;

		public FieldMatcher(final boolean dollar, final Expression name, final PatternMatcher matcher) {
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
				sb.append(((StringLiteral) name).value().asText());
			} else {
				sb.append(name);
			}
			if (matcher != null) {
				sb.append(": ");
				sb.append(matcher);
			}
			return sb.toString();
		}

		public PatternMatcher matcher() {
			if (matcher == null)
				return new ValueMatcher(((StringLiteral) name).value().asText());
			return matcher;
		}
	}

	private void recursive(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			out.accept(accumulate);
			return;
		}

		final FieldMatcher fmatcher = matchers.get(index);
		fmatcher.name.apply(scope, in, (key) -> {
			if (!key.isTextual())
				throw new JsonQueryTypeException("Cannot index %s with %s", in.getNodeType(), key.getNodeType());

			final JsonNode value = in.get(key.asText());

			final int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(Pair.of(key.asText(), value));
			fmatcher.matcher().match(scope, value != null ? value : NullNode.getInstance(), (match) -> {
				recursive(scope, in, out, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	private void recursiveWithPath(final Scope scope, final JsonNode in, final Path inpath, final MatchOutput output, final Stack<MatchWithPath> accumulate, int index) throws JsonQueryException {
		if (index >= matchers.size()) {
			output.emit(accumulate);
			return;
		}

		final FieldMatcher fmatcher = matchers.get(index);
		fmatcher.name.apply(scope, in, (key) -> {
			if (!key.isTextual())
				throw new JsonQueryTypeException("Cannot index %s with %s", in.getNodeType(), key.getNodeType());

			final JsonNode value = in.get(key.asText());
			final Path valuepath = ObjectFieldPath.chainIfNotNull(inpath, key.asText());

			final int size = accumulate.size();
			if (fmatcher.dollar)
				accumulate.push(new MatchWithPath(key.asText(), value, valuepath));
			fmatcher.matcher().matchWithPath(scope, value != null ? value : NullNode.getInstance(), valuepath, (match) -> {
				recursiveWithPath(scope, in, inpath, output, accumulate, index + 1);
			}, accumulate);
			accumulate.setSize(size);
		});
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		if (!in.isObject() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with string", in.getNodeType());

		recursive(scope, in, out, accumulate, 0);
	}

	@Override
	public void matchWithPath(Scope scope, JsonNode in, Path path, MatchOutput output, Stack<MatchWithPath> accumulate) throws JsonQueryException {
		if (!in.isObject() && !in.isNull())
			throw new JsonQueryTypeException("Cannot index %s with string", in.getNodeType());

		recursiveWithPath(scope, in, path, output, accumulate, 0);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		String sep = "";
		for (final FieldMatcher entry : matchers) {
			sb.append(sep);
			sb.append(entry.toString());
			sep = ", ";
		}
		sb.append("}");
		return sb.toString();
	}
}