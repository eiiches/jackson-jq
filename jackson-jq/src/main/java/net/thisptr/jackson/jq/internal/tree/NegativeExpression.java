package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class NegativeExpression implements Expression {
	private Expression value;

	public NegativeExpression(final Expression value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		value.apply(scope, in, (i) -> {
			if (!i.isNumber())
				throw new JsonQueryTypeException(in, "cannot be negated");
			output.emit(JsonNodeUtils.asNumericNode(-i.asDouble()));
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
