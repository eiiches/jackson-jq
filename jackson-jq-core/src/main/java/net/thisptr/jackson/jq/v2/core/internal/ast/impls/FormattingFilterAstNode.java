package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class FormattingFilterAstNode implements AstNode {
	private final String name;

	public FormattingFilterAstNode(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
