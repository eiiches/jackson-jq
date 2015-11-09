package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import net.thisptr.jackson.jq.JsonQuery;

public abstract class ResolvedFieldAccess extends JsonQuery {
	public boolean permissive;

	protected ResolvedFieldAccess(final boolean permissive) {
		this.permissive = permissive;
	}
}