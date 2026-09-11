package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class ValueMatcherAstNode extends AbstractAstNode implements PatternMatcherAstNode {
	private final String name;

	public ValueMatcherAstNode(SourceLocation location, String name) {
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
		return "$" + name;
	}
}
