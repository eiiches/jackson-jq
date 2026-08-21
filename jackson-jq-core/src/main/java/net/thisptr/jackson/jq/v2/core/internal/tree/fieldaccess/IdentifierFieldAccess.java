package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class IdentifierFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private String field;

	public IdentifierFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> obj, String field, boolean permissive) {
		super(jsonProvider, obj, permissive);
		this.field = field;
	}

	public String field() {
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

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		target.apply(frame, in, path, (pobj, ppath) -> {
			emitObjectFieldPath(jsonProvider, permissive, field, pobj, ppath, output, path != null);
		});
	}
}
