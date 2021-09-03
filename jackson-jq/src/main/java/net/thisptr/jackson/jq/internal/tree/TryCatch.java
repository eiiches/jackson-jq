package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class TryCatch implements Expression {
	protected Expression tryExpr;
	protected Expression catchExpr;

	public TryCatch(final Expression tryExpr, final Expression catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(final Expression tryExpr) {
		this(tryExpr, null);
	}

	public TryCatch() {}

	public Expression getCatchExpr() {
		return catchExpr;
	}

	public void setCatchExpr(Expression catchExpr) {
		this.catchExpr = catchExpr;
	}

	public Expression getTryExpr() {
		return tryExpr;
	}

	public void setTryExpr(Expression tryExpr) {
		this.tryExpr = tryExpr;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		try {
			tryExpr.apply(scope, in, path, output, requirePath);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(scope, e.getMessageAsJsonNode(), null, output, requirePath);
			}
		}
	}

	public static class Question extends TryCatch {
		public Question(Expression tryExpr) {
			super(tryExpr);
		}

		@Override
		public String toString() {
			return String.format("(%s)?", tryExpr);
		}
	}

	@Override
	public String toString() {
		if (catchExpr != null) {
			return String.format("(try (%s) catch (%s))", tryExpr, catchExpr);
		} else {
			return String.format("(try (%s))", tryExpr);
		}
	}
}
