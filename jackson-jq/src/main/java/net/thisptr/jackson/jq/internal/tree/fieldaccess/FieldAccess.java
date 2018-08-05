package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;

public abstract class FieldAccess implements Expression {
	protected Expression target;
	protected boolean permissive;

	public FieldAccess(final Expression target, final boolean permissive) {
		this.target = target;
		this.permissive = permissive;
	}

	public static class ResolvedPath {
		public Expression target;
		public List<ResolvedFieldAccess> path;

		public ResolvedPath(final Expression target) {
			this.target = target;
			this.path = new ArrayList<>();
		}

		public ResolvedPath access(final ResolvedFieldAccess path) {
			this.path.add(path);
			return this;
		}
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final ResolvedFieldAccess resolvedFieldAccess = resolveFieldAccess(scope, in);
		target.apply(scope, in, (i) -> {
			resolvedFieldAccess.apply(scope, i, output);
		});
	}

	public abstract ResolvedFieldAccess resolveFieldAccess(final Scope scope, final JsonNode in) throws JsonQueryException;

	public ResolvedPath resolvePath(final Scope scope, final JsonNode in) throws JsonQueryException {
		final ResolvedPath resolvedPath;
		if (target instanceof FieldAccess) {
			resolvedPath = ((FieldAccess) target).resolvePath(scope, in);
		} else {
			resolvedPath = new ResolvedPath(target);
		}
		return resolvedPath.access(resolveFieldAccess(scope, in));
	}
}
