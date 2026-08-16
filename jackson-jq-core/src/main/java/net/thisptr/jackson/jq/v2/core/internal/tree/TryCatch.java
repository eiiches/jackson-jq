package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TryCatch implements Expression {
	protected Expression tryExpr;
	protected @Nullable Expression catchExpr;

	public TryCatch(Expression tryExpr, @Nullable Expression catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(Expression tryExpr) {
		this(tryExpr, null);
	}

	public Expression tryExpr() {
		return tryExpr;
	}

	public @Nullable Expression catchExpr() {
		return catchExpr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		try {
			tryExpr.apply(jsonProvider, frame, in, path, output, requirePath);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(jsonProvider, frame, (JsonNode) e.getMessageAsJsonNode(jsonProvider), null, output, requirePath);
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
