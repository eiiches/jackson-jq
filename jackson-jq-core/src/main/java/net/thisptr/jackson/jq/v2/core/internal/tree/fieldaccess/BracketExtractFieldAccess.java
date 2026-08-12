package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BracketExtractFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	public BracketExtractFieldAccess(Expression<JsonNode> src, boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			emitAllPath(scope.jsonProvider(), permissive, pobj, ppath, output, requirePath);
		}, requirePath);
	}
}
