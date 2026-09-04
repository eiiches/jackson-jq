package net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public abstract class FieldAccessAstNode implements AstNode {
	protected final AstNode target;
	protected final boolean permissive;

	public FieldAccessAstNode(AstNode target, boolean permissive) {
		this.target = target;
		this.permissive = permissive;
	}

	public AstNode target() {
		return target;
	}

	public boolean permissive() {
		return permissive;
	}
}
