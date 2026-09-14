package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
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

	void match(StackFrame frame, JsonNode in, Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException;

	class MatchWithPath<JsonNode> {
		public final int slot;
		public final JsonNode value;
		public final Path<JsonNode> path;

		public MatchWithPath(int slot, JsonNode value, Path<JsonNode> path) {
			this.slot = slot;
			this.value = value;
			this.path = path;
		}
	}

	public interface MatchOutput<JsonNode> {
		void emit(Deque<MatchWithPath<JsonNode>> vars) throws JsonQueryException;
	}

	void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException;

	PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots);
}
