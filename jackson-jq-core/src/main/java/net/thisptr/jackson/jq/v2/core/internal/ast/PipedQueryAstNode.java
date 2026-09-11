package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

public class PipedQueryAstNode extends AbstractAstNode {
	private final AstNode left;
	private final AstNode right;

	public PipedQueryAstNode(SourceLocation location, AstNode left, AstNode right) {
		super(location);
		this.left = left;
		this.right = right;
	}

	public AstNode left() {
		return left;
	}

	public AstNode right() {
		return right;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return left + " | " + right;
	}
}
