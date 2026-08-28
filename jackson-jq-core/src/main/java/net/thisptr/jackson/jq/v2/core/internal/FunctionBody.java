package net.thisptr.jackson.jq.v2.core.internal;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class FunctionBody {
	public static class Builder<Context, JsonNode> {
		private boolean dependsOnExternalState;
		private boolean dependsOnInput;
		private Cardinality cardinality = Cardinality.UNKNOWN;

		private Builder(List<Expression<Context, JsonNode>> boundArguments) {
			this.dependsOnExternalState = boundArguments.stream().anyMatch(Expression::dependsOnExternalState);
			this.dependsOnInput = boundArguments.stream().anyMatch(Expression::dependsOnInput);
		}

		public Builder<Context, JsonNode> usesInput(boolean usesInput) {
			this.dependsOnInput |= usesInput;
			return this;
		}

		public Builder<Context, JsonNode> usesExternalState(boolean usesExternalState) {
			this.dependsOnExternalState |= usesExternalState;
			return this;
		}

		public Builder<Context, JsonNode> cardinality(Cardinality cardinality) {
			this.cardinality = cardinality;
			return this;
		}

		public Expression<Context, JsonNode> build(Expression<Context, JsonNode> expr) {
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
					expr.apply(context, in, ipath, output);
				}
			};
		}
	}

	public static <Context, JsonNode> Builder<Context, JsonNode> builder(List<Expression<Context, JsonNode>> boundArguments) {
		return new Builder<>(boundArguments);
	}
}
