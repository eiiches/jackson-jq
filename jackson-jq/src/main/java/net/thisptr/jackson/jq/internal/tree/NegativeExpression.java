package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class NegativeExpression implements Expression {
	private Expression value;

	public NegativeExpression(final Expression value) {
		this.value = value;
	}

	public NegativeExpression() {}

	public Expression getValue() {
		return value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		value.apply(scope, in, (v) -> {
			if (!v.isNumber())
				throw new JsonQueryTypeException("%s cannot be negated", in);
			output.emit(JsonNodeUtils.asNumericNode(-v.asDouble()), null);
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
