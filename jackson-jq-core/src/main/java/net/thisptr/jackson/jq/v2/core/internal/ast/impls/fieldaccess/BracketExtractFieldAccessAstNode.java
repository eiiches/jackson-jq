package net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class BracketExtractFieldAccessAstNode extends FieldAccessAstNode {
	public BracketExtractFieldAccessAstNode(AstNode src, boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}
}
