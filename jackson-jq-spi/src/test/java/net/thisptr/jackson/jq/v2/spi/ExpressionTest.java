package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class ExpressionTest {
	@Test
	public void expressionIsLambdaCompatible() {
		Expression<RuntimeContext, Object> expression = (state, in, ipath, output) -> {
		};

		assertThat(expression).isNotNull();
	}

	@Test
	public void functionAnalysisDefaultsToUnknown() {
		Function function = new Function() {
			@Override
			public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(
					BindContext<JsonNode> bindContext, java.util.List<Expression<Context, JsonNode>> arguments) {
				return (state, in, ipath, output) -> {
				};
			}
		};

		assertThat(function.analyze(net.thisptr.jackson.jq.v2.spi.version.Version.of(1, 6), java.util.List.of()))
				.isEqualTo(ExpressionProperties.UNKNOWN);
	}

	@Test
	// This test deliberately violates the non-null API contract to verify its runtime guard.
	@SuppressWarnings("NullAway")
	public void expressionPropertiesRequiresACardinality() {
		assertThatNullPointerException()
				.isThrownBy(() -> new ExpressionProperties(null, false, false))
				.withMessage("cardinality");
	}
}
