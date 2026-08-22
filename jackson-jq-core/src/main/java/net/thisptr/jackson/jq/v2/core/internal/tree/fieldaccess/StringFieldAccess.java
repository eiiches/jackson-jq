package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StringFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<JsonNode> field;

	public StringFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> obj, Expression<JsonNode> field, boolean permissive) {
		this(jsonProvider, obj, field, permissive, null);
	}

	public StringFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> obj, Expression<JsonNode> field, boolean permissive, @Nullable Version version) {
		super(jsonProvider, obj, permissive, version);
		this.field = field;
	}

	public Expression<JsonNode> key() {
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
		field.apply(frame, in, null, (key, opath) -> {
			target.apply(frame, in, path, (pobj, ppath) -> {
				if (jsonProvider.getNodeType(key) != JsonNodeType.STRING && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(key), pobj, ppath, output, path != null, version);
			});
		});
	}
}
