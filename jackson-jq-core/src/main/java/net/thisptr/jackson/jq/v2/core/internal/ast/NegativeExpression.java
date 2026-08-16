package net.thisptr.jackson.jq.v2.core.internal.ast;

public class NegativeExpression implements AstNode {
	private AstNode value;

	public NegativeExpression(AstNode value) {
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
