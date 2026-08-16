package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAst;

public class ReduceExpression implements AstNode {
	private AstNode iterExpr;
	private AstNode reduceExpr;
	private AstNode initExpr;
	private PatternMatcherAst matcher;

	public ReduceExpression(PatternMatcherAst matcher, AstNode initExpr, AstNode reduceExpr, AstNode iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcherAst matcher() { return matcher; }
	public AstNode initExpr() { return initExpr; }
	public AstNode reduceExpr() { return reduceExpr; }
	public AstNode iterExpr() { return iterExpr; }

	@Override
	public String toString() {
		return String.format("(reduce %s as %s (%s; %s))", iterExpr, matcher, initExpr, reduceExpr);
	}
}
