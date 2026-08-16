package net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class BracketExtractFieldAccess extends FieldAccess {
	public BracketExtractFieldAccess(AstNode src, boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}
}
