package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

public class IdentifierFieldAccess extends FieldAccess {
	private String field;

	public IdentifierFieldAccess(final Expression obj, final String field, final boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		if (!(target instanceof ThisObject))
			builder.append(target.toString());
		builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}

	@Override
	public ResolvedFieldAccess resolveFieldAccess(final Scope scope, final JsonNode in) {
		return new ResolvedStringFieldAccess(permissive, Collections.singletonList(field));
	}
}
