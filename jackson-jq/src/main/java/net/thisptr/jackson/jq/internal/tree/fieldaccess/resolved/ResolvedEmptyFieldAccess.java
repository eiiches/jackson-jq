package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class ResolvedEmptyFieldAccess extends ResolvedFieldAccess {
	public ResolvedEmptyFieldAccess(final boolean permissive) {
		super(permissive);
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		return Collections.emptyList();
	}
}