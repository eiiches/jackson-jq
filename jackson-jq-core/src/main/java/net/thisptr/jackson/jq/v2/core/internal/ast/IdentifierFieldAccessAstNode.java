package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class IdentifierFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private final String field;

	public IdentifierFieldAccessAstNode(SourceLocation location, AstNode obj, String field, boolean permissive) {
		super(location, obj, permissive);
		this.field = field;
	}

	public String field() {
		return field;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String targetString = target.toString();
		builder.append(targetString);
		if (!".".equals(targetString))
			builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}
}
