package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StringFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<JsonNode> field;

	public StringFieldAccess(Expression<JsonNode> obj, Expression<JsonNode> field, boolean permissive) {
		super(obj, permissive);
		this.field = field;
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

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		field.apply(scope, in, (key) -> {
			target.apply(scope, in, path, (pobj, ppath) -> {
				if (jsonProvider.getNodeType(key) != JsonNodeType.STRING && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(key), pobj, ppath, output, requirePath);
			}, requirePath);
		});
	}
}
