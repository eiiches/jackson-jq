package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAst;

public class ForeachExpression implements AstNode {
	private AstNode iterExpr;
	private AstNode updateExpr;
	private AstNode initExpr;
	private @Nullable AstNode extractExpr;
	private PatternMatcherAst matcher;

	public ForeachExpression(PatternMatcherAst matcher, AstNode initExpr, AstNode updateExpr, @Nullable AstNode extractExpr, AstNode iterExpr) {
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcherAst matcher() { return matcher; }
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
