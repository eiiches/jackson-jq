package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedAllFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;

public class BracketExtractFieldAccess extends FieldAccess {
	public BracketExtractFieldAccess(final Expression src, final boolean permissive) {
		super(src, permissive);
	}

	@Override
	public String toString() {
		return String.format("%s[]%s", target, permissive ? "?" : "");
	}

	@Override
	public ResolvedFieldAccess resolveFieldAccess(final Scope scope, final JsonNode in) throws JsonQueryException {
		return new ResolvedAllFieldAccess(permissive);
	}
}
