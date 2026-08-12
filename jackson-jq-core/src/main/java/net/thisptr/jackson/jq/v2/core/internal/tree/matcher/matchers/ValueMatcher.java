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

	public ValueMatcher(String name) {
		this.name = name;
	}

	@Override
	public void match(Scope<JsonNode> scope, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		accumulate.push(Pair.of(name, in));
		out.accept(accumulate);
	}

	@Override
	public void matchWithPath(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		accumulate.push(new MatchWithPath<>(name, in, path));
		output.emit(accumulate);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
