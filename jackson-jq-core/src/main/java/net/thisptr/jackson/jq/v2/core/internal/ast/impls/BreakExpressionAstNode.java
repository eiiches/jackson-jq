package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class BreakExpressionAstNode implements AstNode {
	private final String name;

	public BreakExpressionAstNode(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "break $" + name;
	}
}
