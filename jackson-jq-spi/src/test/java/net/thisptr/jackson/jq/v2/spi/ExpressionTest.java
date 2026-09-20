package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionTest {
	@Test
	public void cardinalityDefaultsToUnknown() {
		Expression<RuntimeContext, Object> expression = (state, in, ipath, output) -> {
		};

		assertThat(expression.getCardinality()).isEqualTo(Cardinality.UNKNOWN);
	}
}
