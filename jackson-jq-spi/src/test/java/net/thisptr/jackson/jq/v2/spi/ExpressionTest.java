package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.path.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpressionTest {
	@Test
	public void cardinalityDefaultsToUnknown() {
		Expression<RuntimeContext, Object> expression = new Expression<RuntimeContext, Object>() {
			@Override
			public void apply(RuntimeContext state, Object in, @Nullable Path<Object> ipath, Output<Object> output) {
			}
		};

		assertThat(expression.getCardinality()).isEqualTo(Cardinality.UNKNOWN);
	}
}
