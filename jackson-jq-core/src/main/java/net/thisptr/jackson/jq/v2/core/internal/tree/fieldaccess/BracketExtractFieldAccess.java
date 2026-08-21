package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BracketExtractFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	public BracketExtractFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, boolean permissive) {
		this(jsonProvider, src, permissive, null);
	}

	public BracketExtractFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, boolean permissive, @Nullable Version version) {
		super(jsonProvider, src, permissive, version);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		target.apply(frame, in, path, (pobj, ppath) -> {
			emitAllPath(jsonProvider, permissive, pobj, ppath, output, path != null, version);
		});
	}
}
