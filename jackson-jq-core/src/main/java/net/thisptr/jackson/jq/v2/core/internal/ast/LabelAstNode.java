package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

public class LabelAstNode extends AbstractAstNode {
	private final String name;
	private final AstNode body;

	public LabelAstNode(SourceLocation location, String name, AstNode body) {
		super(location);
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
