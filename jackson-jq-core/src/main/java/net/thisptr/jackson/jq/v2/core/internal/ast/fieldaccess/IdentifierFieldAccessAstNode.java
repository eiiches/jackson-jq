package net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;

public class IdentifierFieldAccessAstNode extends FieldAccessAstNode {
	private String field;

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
		if (!(target instanceof ThisObjectAstNode))
			builder.append(target.toString());
		builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}
}
