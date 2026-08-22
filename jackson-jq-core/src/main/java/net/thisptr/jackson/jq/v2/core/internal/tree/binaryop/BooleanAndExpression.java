package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BooleanAndExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;

	public BooleanAndExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> lhs, Expression<JsonNode> rhs) {
		super(lhs, rhs, "and");
		this.jsonProvider = jsonProvider;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		lhs.apply(frame, in, null, (l, opath) -> {
			if (!JsonNodeUtils.asBoolean(jsonProvider, l)) {
				output.emit(jsonProvider.createBoolean(false), null);
				return;
			}
			rhs.apply(frame, in, null, (r, opath2) -> {
				output.emit(jsonProvider.createBoolean(JsonNodeUtils.asBoolean(jsonProvider, r)), null);
			});
		});
	}
}
