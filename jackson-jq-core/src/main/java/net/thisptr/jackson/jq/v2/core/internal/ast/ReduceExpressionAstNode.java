package net.thisptr.jackson.jq.v2.core.internal.ast;


public class ReduceExpressionAstNode implements AstNode {
	private final AstNode iterExpr;
	private final AstNode reduceExpr;
	private final AstNode initExpr;
	private final PatternMatcherAstNode matcher;

	public ReduceExpressionAstNode(PatternMatcherAstNode matcher, AstNode initExpr, AstNode reduceExpr, AstNode iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.reduceExpr = reduceExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcherAstNode matcher() {
		return matcher;
	}

	public AstNode initExpr() {
		return initExpr;
	}

	public AstNode reduceExpr() {
		return reduceExpr;
	}

	public AstNode iterExpr() {
		return iterExpr;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return String.format("reduce %s as %s (%s; %s)", iterExpr, matcher, initExpr, reduceExpr);
	}
}
