package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class ResolvedEmptyFieldAccess extends ResolvedFieldAccess {
	public ResolvedEmptyFieldAccess(final boolean permissive) {
		super(permissive);
	}

	@Override
	public void apply(Scope scope, JsonNode in, final Output output) throws JsonQueryException {
		/* empty */
	}
}