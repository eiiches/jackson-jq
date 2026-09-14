package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public abstract class AbstractFieldAccessAstNode extends AbstractAstNode {
	protected final AstNode target;
	protected final boolean permissive;

	public AbstractFieldAccessAstNode(SourceLocation location, AstNode target, boolean permissive) {
		super(location);
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
