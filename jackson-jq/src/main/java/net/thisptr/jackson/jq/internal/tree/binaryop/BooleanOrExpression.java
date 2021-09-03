package net.thisptr.jackson.jq.internal.tree.binaryop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class BooleanOrExpression extends BinaryOperatorExpression {
	public BooleanOrExpression(final Expression lhs, final Expression rhs) {
		super(lhs, rhs, "or");
	}

	public BooleanOrExpression() {
		super(null, null, "or");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		lhs.apply(scope, in, (l) -> {
			if (JsonNodeUtils.asBoolean(l)) {
				output.emit(BooleanNode.TRUE, null);
				return;
			}
			rhs.apply(scope, in, (r) -> {
				output.emit(BooleanNode.valueOf(JsonNodeUtils.asBoolean(r)), null);
			});
		});
	}
}
