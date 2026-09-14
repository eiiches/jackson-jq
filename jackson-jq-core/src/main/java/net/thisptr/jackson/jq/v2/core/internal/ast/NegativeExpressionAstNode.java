package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class NegativeExpressionAstNode extends AbstractAstNode {
	private final AstNode value;

	public NegativeExpressionAstNode(SourceLocation location, AstNode value) {
		super(location);
		this.value = value;
	}

	public AstNode value() {
		return value;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return "-" + value;
	}
}
