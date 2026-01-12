package net.thisptr.jackson.jq.internal.tree.binaryop;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class BooleanOrExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	public BooleanOrExpression(final Expression<JsonNode> lhs, final Expression<JsonNode> rhs) {
		super(lhs, rhs, "or");
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
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
