package net.thisptr.jackson.jq.v2.core.internal.ast;

public class ParenAstNode implements AstNode {
	private final AstNode value;

	public ParenAstNode(AstNode value) {
		this.value = value;
	}

	public AstNode value() {
		return value;
	}

	@Override
	public String toString() {
		return "(" + value + ")";
	}
}
