package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

final class FunctionBody {
	static final class Builder<Context extends RuntimeContext, JsonNode> {
		private boolean dependsOnExternalState;
		private boolean dependsOnInput;
		private Cardinality cardinality = Cardinality.UNKNOWN;

		private Builder(List<Expression<Context, JsonNode>> boundArguments) {
			dependsOnExternalState = boundArguments.stream().anyMatch(Expression::dependsOnExternalState);
			dependsOnInput = boundArguments.stream().anyMatch(Expression::dependsOnInput);
		}

		Builder<Context, JsonNode> cardinality(Cardinality value) {
			cardinality = value;
			return this;
		}

		Builder<Context, JsonNode> usesInput(boolean value) {
			dependsOnInput |= value;
			return this;
		}

		Expression<Context, JsonNode> build(Expression<Context, JsonNode> expression) {
			return new Expression<Context, JsonNode>() {
				@Override
				public Cardinality getCardinality() {
					return cardinality;
				}

				@Override
				public boolean dependsOnExternalState() {
					return dependsOnExternalState;
				}

				@Override
				public boolean dependsOnInput() {
					return dependsOnInput;
				}

				@Override
				public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
					expression.apply(context, in, ipath, output);
				}
			};
		}
	}

	private FunctionBody() {
	}

	static <Context extends RuntimeContext, JsonNode> Builder<Context, JsonNode> builder(List<Expression<Context, JsonNode>> boundArguments) {
		return new Builder<>(boundArguments);
	}
}
