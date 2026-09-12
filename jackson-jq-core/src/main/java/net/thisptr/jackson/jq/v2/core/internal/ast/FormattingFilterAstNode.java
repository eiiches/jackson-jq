package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class FormattingFilterAstNode extends AbstractAstNode {
	private final String name;

	public FormattingFilterAstNode(SourceLocation location, String name) {
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
		return "@" + name;
	}
}
