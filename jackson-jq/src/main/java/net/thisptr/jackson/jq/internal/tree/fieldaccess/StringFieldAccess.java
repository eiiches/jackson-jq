package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.path.Path;

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
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		field.apply(scope, in, (key) -> {
			target.apply(scope, in, path, (pobj, ppath) -> {
				if (!key.isTextual() && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(permissive, key.asText(), pobj, ppath, output, requirePath);
			}, requirePath);
		});
	}
}
