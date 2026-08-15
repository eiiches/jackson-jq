package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class AssignPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression expr;
	public final PatternMatcher<JsonNode> matcher;
	public final java.util.Map<String, Integer> slots;

	public AssignPipeComponent(Expression expr, PatternMatcher<JsonNode> matcher) {
		this(expr, matcher, java.util.Collections.emptyMap());
	}

	public AssignPipeComponent(Expression expr, PatternMatcher<JsonNode> matcher, java.util.Map<String, Integer> slots) {
		this.expr = expr;
		this.matcher = matcher;
		this.slots = slots;
	}

	public int getSlot(String name) {
		Integer slot = slots.get(name);
		return slot != null ? slot.intValue() : -1;
	}

	@Override
	public boolean canTerminatePipe() {
		return false;
	}

	@Override
	public String toString() {
		return expr + " as " + matcher;
	}
}
