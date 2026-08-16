package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BooleanAndExpression extends BinaryOperatorExpression {
	public BooleanAndExpression(Expression lhs, Expression rhs) {
		super(lhs, rhs, "and");
	}

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		lhs.apply(jsonProvider, frame, in, (l) -> {
			if (!JsonNodeUtils.asBoolean(jsonProvider, l)) {
				output.emit(jsonProvider.createBoolean(false), null);
				return;
			}
			rhs.apply(jsonProvider, frame, in, (r) -> {
				output.emit(jsonProvider.createBoolean(JsonNodeUtils.asBoolean(jsonProvider, r)), null);
			});
		});
	}
}
