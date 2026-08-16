package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
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
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		value.apply(jsonProvider, frame, in, (v) -> {
			if (jsonProvider.getNodeType(v) != JsonNodeType.NUMBER)
				throw new JsonQueryTypeException(jsonProvider, "%s cannot be negated", v);
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, -jsonProvider.asDouble(v)), null);
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
