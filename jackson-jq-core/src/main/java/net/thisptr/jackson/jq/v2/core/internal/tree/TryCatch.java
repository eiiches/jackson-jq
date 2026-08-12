package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TryCatch<JsonNode> implements Expression<JsonNode> {
	protected Expression<JsonNode> tryExpr;
	protected Expression<JsonNode> catchExpr;

	public TryCatch(Expression<JsonNode> tryExpr, Expression<JsonNode> catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(Expression<JsonNode> tryExpr) {
		this(tryExpr, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		try {
			tryExpr.apply(scope, in, path, output, requirePath);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(scope, (JsonNode) e.getMessageAsJsonNode(scope.jsonProvider()), null, output, requirePath);
			}
		}
	}

	public static class Question<JsonNode> extends TryCatch<JsonNode> {
		public Question(Expression<JsonNode> tryExpr) {
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
