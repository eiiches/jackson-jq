package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import net.thisptr.jackson.jq.Expression;

public abstract class ResolvedFieldAccess implements Expression {
	public boolean permissive;

	protected ResolvedFieldAccess(final boolean permissive) {
		this.permissive = permissive;
	}
}