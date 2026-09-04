package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class BracketExtractFieldAccess<JsonNode> extends FieldAccess<JsonNode> {

	@Override
	public Cardinality getCardinality() {
		return target.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN;
	}

	public BracketExtractFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, boolean permissive, Version version) {
		super(jsonProvider, src, permissive, version);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		target.apply(frame, in, path, (pobj, ppath) -> {
			emitAllPath(jsonProvider, permissive, pobj, ppath, output, !(path instanceof UntrackedPath), version);
		});
	}
}
