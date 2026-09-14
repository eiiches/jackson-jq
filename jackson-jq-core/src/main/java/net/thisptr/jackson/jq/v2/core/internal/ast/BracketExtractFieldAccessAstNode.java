package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class BracketExtractFieldAccessAstNode extends AbstractFieldAccessAstNode {
	public BracketExtractFieldAccessAstNode(SourceLocation location, AstNode src, boolean permissive) {
		super(location, src, permissive);
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}
}
