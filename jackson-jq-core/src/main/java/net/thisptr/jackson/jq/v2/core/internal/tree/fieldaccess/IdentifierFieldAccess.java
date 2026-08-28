package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class IdentifierFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private String field;

	@Override
	public Cardinality getCardinality() {
		if (target.getCardinality() == Cardinality.ZERO)
			return Cardinality.ZERO;
		if (!permissive && target.getCardinality() == Cardinality.ONE)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	public IdentifierFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> obj, String field, boolean permissive, Version version) {
		super(jsonProvider, obj, permissive, version);
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		target.apply(frame, in, path, (pobj, ppath) -> {
			emitObjectFieldPath(jsonProvider, permissive, field, pobj, ppath, output, !(path instanceof UntrackedPath), version);
		});
	}
}
