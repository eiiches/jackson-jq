package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * The {@code label $out} head of a pipe. What the label breaks out of is the right-hand side of the
 * {@link PipeAstNode} it heads, not a child of its own.
 */
public class LabelAstNode extends AbstractAstNode {
	private final String name;

	public LabelAstNode(SourceLocation location, String name) {
		super(location);
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return "label $" + name;
	}
}
