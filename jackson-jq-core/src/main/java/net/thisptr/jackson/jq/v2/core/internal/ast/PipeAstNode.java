package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * A {@code |}. The left side is an ordinary expression, or one of the two pipe heads that scope
 * everything after the {@code |}: {@link AsBindingAstNode} or {@link LabelAstNode}.
 */
public class PipeAstNode extends AbstractAstNode {
	private final AstNode left;
	private final AstNode right;

	public PipeAstNode(SourceLocation location, AstNode left, AstNode right) {
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
