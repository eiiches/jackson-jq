package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class NegativeExpression implements Expression {
	private Expression value;

	public NegativeExpression(Expression value) {
		this.value = value;
	}

	public Expression value() {
		return value;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
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
