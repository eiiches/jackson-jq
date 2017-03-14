package net.thisptr.jackson.jq.internal.tree.matcher.matchers;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class ValueMatcher implements PatternMatcher {
	private String name;

	public ValueMatcher(final String name) {
		this.name = name;
	}

	@Override
	public void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException {
		accumulate.push(Pair.of(name, in));
		if (emit)
			out.accept(accumulate);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}