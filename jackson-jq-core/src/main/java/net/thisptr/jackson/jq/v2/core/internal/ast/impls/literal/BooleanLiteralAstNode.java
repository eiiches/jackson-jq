package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

public class BooleanLiteralAstNode extends AbstractValueLiteralAstNode {
	private final boolean value;

	public BooleanLiteralAstNode(boolean value) {
		this.value = value;
	}

	public boolean value() {
		return value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
