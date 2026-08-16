package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class AlternativeOperatorExpression<JsonNode> extends BinaryOperatorExpression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;

	public AlternativeOperatorExpression(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> valueExpr, Expression<JsonNode> defaultExpr) {
		super(valueExpr, defaultExpr, "//");
		this.jsonProvider = jsonProvider;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		AtomicBoolean emitted = new AtomicBoolean();
		lhs.apply(frame, in, path, (out, outpath) -> {
			if (JsonNodeUtils.asBoolean(jsonProvider, out)) {
				output.emit(out, outpath);
				emitted.set(true);
			}
		}, requirePath);
		if (!emitted.get()) {
			rhs.apply(frame, in, path, output, requirePath);
		}
	}
}
