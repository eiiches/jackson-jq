package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

public class BooleanLiteralAstNode extends AbstractValueLiteralAstNode {
	private final boolean value;

	public BooleanLiteralAstNode(SourceLocation location, boolean value) {
		super(location);
		this.value = value;
	}

	public boolean value() {
		return value;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
