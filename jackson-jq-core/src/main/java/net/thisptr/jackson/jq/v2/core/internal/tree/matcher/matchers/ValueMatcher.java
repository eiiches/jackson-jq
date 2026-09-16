package net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.SlotResolver;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ValueMatcher<JsonNode> implements PatternMatcher<JsonNode> {
	private final String name;

	/**
	 * The frame slot to bind, or -1 for an occurrence a previous one shadows (see {@link SlotResolver#claim}).
	 */
	private final int slot;

	public ValueMatcher(String name) {
		this(name, -1);
	}

	ValueMatcher(String name, int slot) {
		this.name = name;
		this.slot = slot;
	}

	@Override
	public void match(StackFrame frame, JsonNode in, OnMatch onMatch) throws JsonQueryException {
		if (slot >= 0)
			frame.set(slot, StackFrameValues.toSlot(in));
		onMatch.matched();
	}

	@Override
	public void matchWithPath(StackFrame frame, JsonNode in, Path<JsonNode> path, OnMatch onMatch) throws JsonQueryException {
		if (slot >= 0)
			frame.set(slot, StackFrameValues.toSlot(in, path));
		onMatch.matched();
	}

	@Override
	public PatternMatcher<JsonNode> resolveSlots(SlotResolver resolver) {
		return new ValueMatcher<>(name, resolver.claim(name));
	}
}
