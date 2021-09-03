package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class BracketExtractFieldAccess extends FieldAccess {

	public BracketExtractFieldAccess() {}

	public BracketExtractFieldAccess(final Expression src, final boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			emitAllPath(permissive, pobj, ppath, output, requirePath);
		}, requirePath);
	}
}
