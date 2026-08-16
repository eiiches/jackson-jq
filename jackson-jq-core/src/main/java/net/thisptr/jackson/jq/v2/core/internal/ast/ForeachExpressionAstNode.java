package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAstNode;

public class ForeachExpressionAstNode implements AstNode {
	private AstNode iterExpr;
	private AstNode updateExpr;
	private AstNode initExpr;
	private @Nullable AstNode extractExpr;
	private PatternMatcherAstNode matcher;

	public ForeachExpressionAstNode(PatternMatcherAstNode matcher, AstNode initExpr, AstNode updateExpr, @Nullable AstNode extractExpr, AstNode iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcherAstNode matcher() { return matcher; }
	public AstNode initExpr() { return initExpr; }
	public AstNode updateExpr() { return updateExpr; }
	public @Nullable AstNode extractExpr() { return extractExpr; }
	public AstNode iterExpr() { return iterExpr; }

	@Override
	public String toString() {
		if (extractExpr == null) {
			return String.format("(foreach %s as %s (%s; %s))", iterExpr, matcher, initExpr, updateExpr);
		} else {
			return String.format("(foreach %s as %s (%s; %s; %s))", iterExpr, matcher, initExpr, updateExpr, extractExpr);
		}
	}
}
