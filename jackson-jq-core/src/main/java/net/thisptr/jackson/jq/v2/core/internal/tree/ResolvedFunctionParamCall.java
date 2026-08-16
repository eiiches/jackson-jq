package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionParamCall implements Expression {
	private final Expression paramExpr;

	public ResolvedFunctionParamCall(Expression paramExpr) {
		this.paramExpr = paramExpr;
	}

	public Expression paramExpr() {
		return paramExpr;
	}

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		paramExpr.apply(jsonProvider, frame, in, ipath, output, requirePath);
	}

	@Override
	public String toString() {
		return paramExpr.toString();
	}
}
