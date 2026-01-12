package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.ThisObject;
import net.thisptr.jackson.jq.path.Path;

public class StringFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<JsonNode> field;

	public StringFieldAccess(final Expression<JsonNode> obj, final Expression<JsonNode> field, final boolean permissive) {
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
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		field.apply(scope, in, (key) -> {
			target.apply(scope, in, path, (pobj, ppath) -> {
				if (jsonProvider.getNodeType(key) != JsonNodeType.STRING && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(key), pobj, ppath, output, requirePath);
			}, requirePath);
		});
	}
}
