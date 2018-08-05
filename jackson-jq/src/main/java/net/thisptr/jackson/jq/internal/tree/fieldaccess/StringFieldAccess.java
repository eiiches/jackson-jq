package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

public class StringFieldAccess extends FieldAccess {
	private Expression field;

	public StringFieldAccess(final Expression obj, final Expression field, final boolean permissive) {
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
	public ResolvedFieldAccess resolveFieldAccess(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<String> keys = new ArrayList<>();
		field.apply(scope, in, (key) -> {
			if (!key.isTextual())
				throw new IllegalStateException();
			keys.add(key.asText());
		});
		return new ResolvedStringFieldAccess(permissive, keys);
	}
}
