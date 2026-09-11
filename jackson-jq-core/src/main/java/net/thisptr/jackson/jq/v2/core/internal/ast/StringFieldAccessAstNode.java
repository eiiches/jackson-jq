package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class StringFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private final AstNode field;

	public StringFieldAccessAstNode(SourceLocation location, AstNode obj, AstNode field, boolean permissive) {
		super(location, obj, permissive);
		this.field = field;
	}

	public AstNode key() {
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
