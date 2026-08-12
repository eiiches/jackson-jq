package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.concurrent.atomic.AtomicBoolean;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class AlternativeOperatorExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	public AlternativeOperatorExpression(Expression<JsonNode> valueExpr, Expression<JsonNode> defaultExpr) {
		super(valueExpr, defaultExpr, "//");
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		AtomicBoolean emitted = new AtomicBoolean();
		lhs.apply(scope, in, path, (out, outpath) -> {
			if (JsonNodeUtils.asBoolean(scope.jsonProvider(), out)) {
				output.emit(out, outpath);
				emitted.set(true);
			}
		}, requirePath);
		if (!emitted.get()) {
			rhs.apply(scope, in, path, output, requirePath);
		}
	}
}
