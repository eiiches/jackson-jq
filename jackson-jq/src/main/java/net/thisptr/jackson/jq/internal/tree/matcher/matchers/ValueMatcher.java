package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.path.Path;

public class ValueMatcher implements PatternMatcher {
	private String name;

	public ValueMatcher(final String name) {
		this.name = name;
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException {
		accumulate.push(Pair.of(name, in));
		out.accept(accumulate);
	}

	@Override
	public void matchWithPath(final Scope scope, final JsonNode in, final Path path, final MatchOutput output, final Stack<MatchWithPath> accumulate) throws JsonQueryException {
		accumulate.push(new MatchWithPath(name, in, path));
		output.emit(accumulate);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}