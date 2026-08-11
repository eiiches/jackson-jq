package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ValueMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private String name;

	public ValueMatcher(final String name) {
		this.name = name;
	}

	@Override
	public void match(final Scope<JsonNode> scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		accumulate.push(Pair.of(name, in));
		out.accept(accumulate);
	}

	@Override
	public void matchWithPath(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final MatchOutput<JsonNode> output, final Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		accumulate.push(new MatchWithPath<>(name, in, path));
		output.emit(accumulate);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
