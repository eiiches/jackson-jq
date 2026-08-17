package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class AssignPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression<JsonNode> expr;
	public final PatternMatcher<JsonNode> matcher;
	public final Map<String, Integer> slots;

	public AssignPipeComponent(Expression<JsonNode> expr, PatternMatcher<JsonNode> matcher) {
		this(expr, matcher, Collections.emptyMap());
	}

	public AssignPipeComponent(Expression<JsonNode> expr, PatternMatcher<JsonNode> matcher, Map<String, Integer> slots) {
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
