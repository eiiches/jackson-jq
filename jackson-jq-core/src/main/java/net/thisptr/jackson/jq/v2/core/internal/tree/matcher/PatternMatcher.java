package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.List;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface PatternMatcher<JsonNode> {
	void match(Scope<JsonNode> scope, JsonNode in, Functional.Consumer<List<Pair<String, JsonNode>>> out, Stack<Pair<String, JsonNode>> accumulate) throws JsonQueryException;

	class MatchWithPath<JsonNode> {
		public String name;
		public JsonNode value;
		public @Nullable Path<JsonNode> path;

		public MatchWithPath(String name, JsonNode value, @Nullable Path<JsonNode> path) {
			this.name = name;
			this.value = value;
			this.path = path;
		}
	}

	public interface MatchOutput<JsonNode> {
		void emit(List<MatchWithPath<JsonNode>> vars) throws JsonQueryException;
	}

	void matchWithPath(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException;
}
