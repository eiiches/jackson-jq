package net.thisptr.jackson.jq.internal.tree.matcher;

import java.util.List;
import java.util.Stack;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public interface PatternMatcher<JsonNode> {
	void match(final Scope<JsonNode> scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException;

	public static class MatchWithPath<JsonNode> {
		public final String name;
		public final JsonNode value;
		public final Path<JsonNode> path;

		public MatchWithPath(final String name, final JsonNode value, final Path<JsonNode> path) {
			this.name = name;
			this.value = value;
			this.path = path;
		}
	}

	public interface MatchOutput<JsonNode> {
		void emit(List<MatchWithPath<JsonNode>> vars) throws JsonQueryException;
	}

	void matchWithPath(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException;
}
