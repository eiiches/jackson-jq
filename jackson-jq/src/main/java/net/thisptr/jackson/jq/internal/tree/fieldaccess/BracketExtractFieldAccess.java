package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class BracketExtractFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	public BracketExtractFieldAccess(final Expression<JsonNode> src, final boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			emitAllPath(scope.jsonProvider(), permissive, pobj, ppath, output, requirePath);
		}, requirePath);
	}
}
