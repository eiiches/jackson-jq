package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

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
