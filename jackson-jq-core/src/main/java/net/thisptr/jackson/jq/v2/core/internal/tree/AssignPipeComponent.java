package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class AssignPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final Expression<StackFrame, JsonNode> expr;
	public final PatternMatcher<JsonNode> matcher;
	/**
	 * Local slots this component's matcher binds, used by PipedQuery to close its free variables.
	 */
	public final Set<Integer> boundSlots;

	public AssignPipeComponent(Expression<StackFrame, JsonNode> expr, PatternMatcher<JsonNode> matcher, Set<Integer> boundSlots) {
		this.expr = expr;
		this.matcher = matcher;
		this.boundSlots = boundSlots;
	}
}
