package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class ForeachExpressionAstNode extends AbstractAstNode {
	private final AstNode iterExpr;
	private final AstNode updateExpr;
	private final AstNode initExpr;
	private final @Nullable AstNode extractExpr;
	private final PatternMatcherAstNode matcher;

	public ForeachExpressionAstNode(SourceLocation location, PatternMatcherAstNode matcher, AstNode initExpr, AstNode updateExpr, @Nullable AstNode extractExpr, AstNode iterExpr) {
		super(location);
		this.matcher = matcher;
		this.initExpr = initExpr;
		this.updateExpr = updateExpr;
		this.extractExpr = extractExpr;
		this.iterExpr = iterExpr;
	}

	public PatternMatcherAstNode matcher() {
		return matcher;
	}

	public AstNode initExpr() {
		return initExpr;
	}

	public AstNode updateExpr() {
		return updateExpr;
	}

	public @Nullable AstNode extractExpr() {
		return extractExpr;
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
		if (extractExpr == null) {
			return String.format("foreach %s as %s (%s; %s)", iterExpr, matcher, initExpr, updateExpr);
		} else {
			return String.format("foreach %s as %s (%s; %s; %s)", iterExpr, matcher, initExpr, updateExpr, extractExpr);
		}
	}
}
