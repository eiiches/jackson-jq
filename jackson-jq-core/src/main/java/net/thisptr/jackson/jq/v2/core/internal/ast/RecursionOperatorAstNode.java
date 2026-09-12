package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class RecursionOperatorAstNode extends AbstractAstNode {

	public RecursionOperatorAstNode(SourceLocation location) {
		super(location);
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return "(..)";
	}
}
