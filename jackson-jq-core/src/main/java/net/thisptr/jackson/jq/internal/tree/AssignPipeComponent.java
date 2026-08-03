package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class AssignPipeComponent implements PipeComponent {
	public final Expression expr;
	public final PatternMatcher matcher;

	public AssignPipeComponent(final Expression expr, final PatternMatcher matcher) {
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
