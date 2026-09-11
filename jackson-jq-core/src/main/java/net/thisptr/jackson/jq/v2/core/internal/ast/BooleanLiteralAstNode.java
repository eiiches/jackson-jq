package net.thisptr.jackson.jq.v2.core.internal.ast;

public class BooleanLiteralAstNode extends AbstractValueLiteralAstNode {
	private final boolean value;

	public BooleanLiteralAstNode(boolean value) {
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
