package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class NegativeExpression<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<JsonNode> value;
	private final @Nullable Version version;

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> value) {
		this(jsonProvider, value, null);
	}

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> value, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.value = value;
		this.version = version;
	}

	public Expression<JsonNode> value() {
		return value;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output) throws JsonQueryException {
		value.apply(frame, in, (v) -> {
			if (jsonProvider.getNodeType(v) != JsonNodeType.NUMBER)
				throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be negated", v);
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, -jsonProvider.asDouble(v)), null);
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
