package net.thisptr.jackson.jq.v2.core.internal.ast;


public class BracketExtractFieldAccessAstNode extends AbstractFieldAccessAstNode {
	public BracketExtractFieldAccessAstNode(AstNode src, boolean permissive) {
		super(src, permissive);
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
