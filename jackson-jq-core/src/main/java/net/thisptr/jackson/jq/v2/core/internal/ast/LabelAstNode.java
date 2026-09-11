package net.thisptr.jackson.jq.v2.core.internal.ast;

public class LabelAstNode implements AstNode {
	private final String name;
	private final AstNode body;

	public LabelAstNode(String name, AstNode body) {
		this.name = name;
		this.body = body;
	}

	public String name() {
		return name;
	}

	public AstNode body() {
		return body;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return "label $" + name + " | " + body;
	}
}
