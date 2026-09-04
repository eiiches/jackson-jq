package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

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
		return "-" + value;
	}
}
