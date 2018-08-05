package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class AlternativeOperator extends BinaryOperatorExpression {
	public AlternativeOperator(final Expression valueExpr, final Expression defaultExpr) {
		super(valueExpr, defaultExpr, "//");
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		final AtomicBoolean emitted = new AtomicBoolean();
		lhs.apply(scope, in, (out) -> {
			if (JsonNodeUtils.asBoolean(out)) {
				output.emit(out);
				emitted.set(true);
			}
		});
		if (!emitted.get()) {
			rhs.apply(scope, in, output);
		}
	}
}
