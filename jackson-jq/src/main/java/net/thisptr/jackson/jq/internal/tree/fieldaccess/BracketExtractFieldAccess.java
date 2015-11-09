package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedAllFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;

import com.fasterxml.jackson.databind.JsonNode;

public class BracketExtractFieldAccess extends FieldAccess {
	public BracketExtractFieldAccess(final JsonQuery src, final boolean permissive) {
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
