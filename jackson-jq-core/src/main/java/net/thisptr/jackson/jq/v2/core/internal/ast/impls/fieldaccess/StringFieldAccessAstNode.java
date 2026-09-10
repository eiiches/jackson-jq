package net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class StringFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private AstNode field;

	public StringFieldAccessAstNode(AstNode obj, AstNode field, boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	public AstNode key() {
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
