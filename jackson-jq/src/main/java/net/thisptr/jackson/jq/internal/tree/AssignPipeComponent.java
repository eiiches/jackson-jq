package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.tree.matcher.PatternMatcher;

public class AssignPipeComponent implements PipeComponent {
	public Expression expr;
	public PatternMatcher matcher;

	public AssignPipeComponent(final Expression expr, final PatternMatcher matcher) {
		this.expr = expr;
		this.matcher = matcher;
	}

	public AssignPipeComponent() {

	}

	public Expression getExpr() {
		return expr;
	}

	public void setExpr(Expression expr) {
		this.expr = expr;
	}

	public PatternMatcher getMatcher() {
		return matcher;
	}

	public void setMatcher(PatternMatcher matcher) {
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
