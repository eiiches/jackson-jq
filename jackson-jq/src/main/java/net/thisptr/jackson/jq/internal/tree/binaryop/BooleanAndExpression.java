package net.thisptr.jackson.jq.internal.tree.binaryop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class BooleanAndExpression extends BinaryOperatorExpression {
	public BooleanAndExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "and");
	}

	public BooleanAndExpression() {}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		lhs.apply(scope, in, (l) -> {
			if (!JsonNodeUtils.asBoolean(l)) {
				output.emit(BooleanNode.FALSE, null);
				return;
			}
			rhs.apply(scope, in, (r) -> {
				output.emit(BooleanNode.valueOf(JsonNodeUtils.asBoolean(r)), null);
			});
		});
	}
}
