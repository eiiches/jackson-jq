package net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class IdentifierFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private final String field;

	public IdentifierFieldAccessAstNode(AstNode obj, String field, boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	public String field() {
		return field;
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
