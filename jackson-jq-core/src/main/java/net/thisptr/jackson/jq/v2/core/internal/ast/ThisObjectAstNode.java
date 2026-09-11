package net.thisptr.jackson.jq.v2.core.internal.ast;


public class ThisObjectAstNode implements AstNode {

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return ".";
	}
}
