package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BooleanOrExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	public BooleanOrExpression(Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, "or");
	}

	@Override
	public void apply(Scope<JsonNode> scope, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		lhs.apply(scope, in, (l) -> {
			if (JsonNodeUtils.asBoolean(scope.jsonProvider(), l)) {
				output.emit(scope.jsonProvider().createBoolean(true), null);
				return;
			}
			rhs.apply(scope, in, (r) -> {
				output.emit(scope.jsonProvider().createBoolean(JsonNodeUtils.asBoolean(scope.jsonProvider(), r)), null);
			});
		});
	}
}
