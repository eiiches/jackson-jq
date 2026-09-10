package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class IdentifierFieldAccess<JsonNode> extends AbstractFieldAccess<JsonNode> {
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

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		target.apply(frame, in, path, (pobj, ppath) -> {
			emitObjectFieldPath(jsonProvider, permissive, field, pobj, ppath, output, !(path instanceof UntrackedPath), version);
		});
	}
}
