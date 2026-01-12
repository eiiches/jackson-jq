package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class TryCatch<JsonNode> implements Expression<JsonNode> {
	protected Expression<JsonNode> tryExpr;
	protected Expression<JsonNode> catchExpr;

	public TryCatch(final Expression<JsonNode> tryExpr, final Expression<JsonNode> catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(final Expression<JsonNode> tryExpr) {
		this(tryExpr, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
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
