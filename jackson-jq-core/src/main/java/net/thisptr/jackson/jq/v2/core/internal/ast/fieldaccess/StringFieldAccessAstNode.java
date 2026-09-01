package net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.ThisObjectAstNode;

public class StringFieldAccessAstNode extends FieldAccessAstNode {
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
		if (!(target instanceof ThisObjectAstNode))
			builder.append(target.toString());
		builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}
}
