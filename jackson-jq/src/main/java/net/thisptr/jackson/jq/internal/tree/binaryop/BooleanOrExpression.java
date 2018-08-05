package net.thisptr.jackson.jq.internal.tree.binaryop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class BooleanOrExpression extends BinaryOperatorExpression {
	public BooleanOrExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "or");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		lhs.apply(scope, in, (l) -> {
			if (JsonNodeUtils.asBoolean(l)) {
				output.emit(BooleanNode.TRUE);
				return;
			}
			rhs.apply(scope, in, (r) -> {
				output.emit(BooleanNode.valueOf(JsonNodeUtils.asBoolean(r)));
			});
		});
	}
}
