package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import java.util.Deque;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.Functional;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ValueMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final String name;
	private final int slot;

	public ValueMatcher(String name) {
		this(name, -1);
	}

	ValueMatcher(String name, int slot) {
		this.name = name;
		this.slot = slot;
	}

	public String name() {
		return name;
	}

	@Override
	public void match(StackFrame frame, JsonNode in, Functional.Consumer<Deque<Match<JsonNode>>> out, Deque<Match<JsonNode>> accumulate) throws JsonQueryException {
		accumulate.addLast(new Match<>(slot, in));
		out.accept(accumulate);
		accumulate.removeLast();
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, MatchOutput<JsonNode> output, Deque<MatchWithPath<JsonNode>> accumulate) throws JsonQueryException {
		accumulate.addLast(new MatchWithPath<>(slot, in, path));
		output.emit(accumulate);
		accumulate.removeLast();
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(Map<String, Integer> slots) {
		Integer resolvedSlot = slots.get(name);
		if (resolvedSlot == null)
			throw new IllegalStateException("No slot allocated for pattern variable $" + name);
		return new ValueMatcher<>(name, resolvedSlot.intValue());
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
