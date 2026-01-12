package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

public class NegativeExpression<JsonNode> implements Expression<JsonNode> {
	private Expression<JsonNode> value;

	public NegativeExpression(final Expression<JsonNode> value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		value.apply(scope, in, (v) -> {
			if (scope.jsonProvider().getNodeType(v) != JsonNodeType.NUMBER)
				throw new JsonQueryTypeException(scope.jsonProvider(), "%s cannot be negated", v);
			output.emit(JsonNodeUtils.asNumericNode(scope.jsonProvider(), -scope.jsonProvider().asDouble(v)), null);
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
