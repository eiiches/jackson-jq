package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TryCatch<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	protected Expression<JsonNode> tryExpr;
	protected @Nullable Expression<JsonNode> catchExpr;

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> tryExpr, @Nullable Expression<JsonNode> catchExpr) {
		this.jsonProvider = jsonProvider;
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> tryExpr) {
		this(jsonProvider, tryExpr, null);
	}

	public Expression<JsonNode> tryExpr() {
		return tryExpr;
	}

	public @Nullable Expression<JsonNode> catchExpr() {
		return catchExpr;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		try {
			tryExpr.apply(frame, in, path, output, requirePath);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(frame, e.getMessageAsJsonNode(jsonProvider), null, output, requirePath);
			}
		}
	}

	public static class Question<JsonNode> extends TryCatch<JsonNode> {
		public Question(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> tryExpr) {
			super(jsonProvider, tryExpr);
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
