package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.path.Path;

public class IdentifierFieldAccess extends FieldAccess {
	private String field;

	public IdentifierFieldAccess(final Expression obj, final String field, final boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	public IdentifierFieldAccess() {}

	public String getField() {
		return field;
	}

	public void setField(String field) {
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
	public void apply(Scope scope, JsonNode in, Path path, PathOutput output, boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			emitObjectFieldPath(permissive, field, pobj, ppath, output, requirePath);
		}, requirePath);
	}
}
