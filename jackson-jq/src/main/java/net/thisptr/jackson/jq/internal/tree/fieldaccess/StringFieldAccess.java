package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

import com.fasterxml.jackson.databind.JsonNode;

public class StringFieldAccess extends FieldAccess {
	private JsonQuery field;

	public StringFieldAccess(final JsonQuery obj, final JsonQuery field, final boolean permissive) {
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
		for (final JsonNode key : field.apply(scope, in)) {
			if (!key.isTextual())
				throw new IllegalStateException();
			keys.add(key.asText());
		}
		return new ResolvedStringFieldAccess(permissive, keys);
	}
}
