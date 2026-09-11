package net.thisptr.jackson.jq.v2.core.internal.ast;


public class NegativeExpressionAstNode implements AstNode {
	private final AstNode value;

	public NegativeExpressionAstNode(AstNode value) {
		this.value = value;
	}

	public AstNode value() {
		return value;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return "-" + value;
	}
}
