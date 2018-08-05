package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

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

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		try {
			tryExpr.apply(scope, in, output);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(scope, e.getMessageAsJsonNode(), output);
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
