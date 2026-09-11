package net.thisptr.jackson.jq.v2.core.internal.ast;


public class BreakExpressionAstNode implements AstNode {
	private final String name;

	public BreakExpressionAstNode(String name) {
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
		return "break $" + name;
	}
}
