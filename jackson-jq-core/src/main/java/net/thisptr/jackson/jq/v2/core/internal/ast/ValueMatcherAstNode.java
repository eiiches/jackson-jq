package net.thisptr.jackson.jq.v2.core.internal.ast;


public class ValueMatcherAstNode implements PatternMatcherAstNode {
	private final String name;

	public ValueMatcherAstNode(String name) {
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
		return "$" + name;
	}
}
