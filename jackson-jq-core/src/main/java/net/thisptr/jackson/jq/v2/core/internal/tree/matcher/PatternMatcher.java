package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface PatternMatcher<JsonNode> {
	class Match<JsonNode> {
		public final int slot;
		public final JsonNode value;

		public Match(int slot, JsonNode value) {
			this.slot = slot;
			this.value = value;
		}
	}

	void match(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<List<Match<JsonNode>>> out, Stack<Match<JsonNode>> accumulate) throws JsonQueryException;

	class MatchWithPath<JsonNode> {
		public final int slot;
		public final JsonNode value;
		public final @Nullable Path<JsonNode> path;

		public MatchWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path) {
			this.slot = slot;
			this.value = value;
			this.path = path;
		}
	}

	public interface MatchOutput<JsonNode> {
		void emit(List<MatchWithPath<JsonNode>> vars) throws JsonQueryException;
	}

	void matchWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> output, Stack<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException;

	PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots);
}
