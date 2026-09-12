package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * A {@code ,}. Left-associated, as in jq: {@code a, b, c} is {@code (a, b), c}. The nesting carries
 * no meaning of its own -- both sides see the same input and the same path, and their outputs are
 * concatenated -- so either association would evaluate the same way.
 */
public class CommaAstNode extends AbstractAstNode {
	private final AstNode left;
	private final AstNode right;

	public CommaAstNode(SourceLocation location, AstNode left, AstNode right) {
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
		return left + ", " + right;
	}
}
