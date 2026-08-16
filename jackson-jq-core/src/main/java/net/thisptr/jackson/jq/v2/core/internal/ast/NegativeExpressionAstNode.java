package net.thisptr.jackson.jq.v2.core.internal.ast;

public class NegativeExpressionAstNode implements AstNode {
	private AstNode value;

	public NegativeExpressionAstNode(AstNode value) {
		this.value = value;
	}

	public AstNode value() {
		return value;
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
