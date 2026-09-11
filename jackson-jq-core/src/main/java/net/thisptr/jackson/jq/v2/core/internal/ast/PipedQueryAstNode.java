package net.thisptr.jackson.jq.v2.core.internal.ast;

public class PipedQueryAstNode implements AstNode {
	private final AstNode left;
	private final AstNode right;

	public PipedQueryAstNode(AstNode left, AstNode right) {
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
