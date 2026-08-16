package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StringFieldAccess extends FieldAccess {
	private Expression field;

	public StringFieldAccess(Expression obj, Expression field, boolean permissive) {
		super(obj, permissive);
		this.field = field;
	}

	public Expression key() {
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
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		field.apply(jsonProvider, frame, in, (key) -> {
			target.apply(jsonProvider, frame, in, path, (pobj, ppath) -> {
				if (jsonProvider.getNodeType(key) != JsonNodeType.STRING && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(key), pobj, ppath, output, requirePath);
			}, requirePath);
		});
	}
}
