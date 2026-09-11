package net.thisptr.jackson.jq.v2.core.internal.ast;


public abstract class AbstractFieldAccessAstNode implements AstNode {
	protected final AstNode target;
	protected final boolean permissive;

	public AbstractFieldAccessAstNode(AstNode target, boolean permissive) {
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
