package net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;

public class StringFieldAccess extends FieldAccess {
	private AstNode field;

	public StringFieldAccess(AstNode obj, AstNode field, boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	public AstNode key() {
		return field;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (!(target instanceof ThisObject))
			builder.append(target.toString());
		builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}
}
