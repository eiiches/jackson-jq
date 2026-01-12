package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class AssignPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression<JsonNode> expr;
	public final PatternMatcher<JsonNode> matcher;

	public AssignPipeComponent(final Expression<JsonNode> expr, final PatternMatcher<JsonNode> matcher) {
		this.expr = expr;
		this.matcher = matcher;
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
