package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.path.Path;

public class IdentifierFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private String field;

	public IdentifierFieldAccess(final Expression<JsonNode> obj, final String field, final boolean permissive) {
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
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			emitObjectFieldPath(scope.jsonProvider(), permissive, field, pobj, ppath, output, requirePath);
		}, requirePath);
	}
}
