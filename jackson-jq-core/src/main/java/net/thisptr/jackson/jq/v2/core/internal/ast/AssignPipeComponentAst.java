package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAst;

public class AssignPipeComponentAst implements PipeComponentAst {
	public final AstNode expr;
	public final PatternMatcherAst matcher;

	public AssignPipeComponentAst(AstNode expr, PatternMatcherAst matcher) {
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
