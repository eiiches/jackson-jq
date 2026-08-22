package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.Deque;
import java.util.Map;

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

	void match(@Nullable StackFrame frame, JsonNode in, Functional.Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException;

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
		void emit(Deque<MatchWithPath<JsonNode>> vars) throws JsonQueryException;
	}

	void matchWithPath(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, MatchOutput<JsonNode> output, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException;

	PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots);
}
