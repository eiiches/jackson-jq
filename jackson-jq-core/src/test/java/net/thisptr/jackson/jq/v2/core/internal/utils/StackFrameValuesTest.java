package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;

public class StackFrameValuesTest {

	@Test
	void passesThroughAnExistingPathAndValue() {
		PathAndValue<String> pv = new PathAndValue<>(UntrackedPath.getInstance(), "value");

		assertThat(StackFrameValues.<String>asPathAndValue(pv)).isSameAs(pv);
	}

	@Test
	void wrapsAPlainValueWithoutAPath() {
		PathAndValue<String> result = StackFrameValues.asPathAndValue("value");

		assertThat(result).isNotNull();
		assertThat(Objects.requireNonNull(result).getPath()).isSameAs(UntrackedPath.getInstance());
		assertThat(result.getValue()).isSameAs("value");
	}

	@Test
	void returnsNullForNullRawValue() {
		assertThat(StackFrameValues.asPathAndValue(null)).isNull();
	}

	@Test
	void roundTripsAPlainValueThroughASlot() {
		PathAndValue<String> result = StackFrameValues.asPathAndValue(StackFrameValues.toSlot("value"));

		assertThat(result).isNotNull();
		assertThat(Objects.requireNonNull(result).getPath()).isSameAs(UntrackedPath.getInstance());
		assertThat(result.getValue()).isSameAs("value");
	}

	// A provider representing JSON null as Java null binds exactly this. It must not be stored as a
	// bare null, which a slot reads as an unset variable. NullAway cannot express such a value, which
	// is the whole reason the slot encoding exists.
	@SuppressWarnings("NullAway")
	@Test
	void roundTripsAJavaNullValueThroughASlotAsAValue() {
		Object slot = StackFrameValues.toSlot((String) null);
		assertThat(slot).isNotNull();

		PathAndValue<String> result = StackFrameValues.asPathAndValue(slot);

		assertThat(result).isNotNull();
		assertThat(result.getPath()).isSameAs(UntrackedPath.getInstance());
		assertThat(result.getValue()).isNull();
	}

	@Test
	void returnsNullForFunctionRawValue() {
		Function factory = new Function() {
			@Override
			public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> args) {
				throw new UnsupportedOperationException();
			}
		};

		assertThat(StackFrameValues.asPathAndValue(factory)).isNull();
	}

	@Test
	void returnsNullForExpressionRawValue() {
		Expression<RuntimeContext, String> expression = (frame, in, path, output) -> {
			throw new UnsupportedOperationException();
		};

		assertThat(StackFrameValues.asPathAndValue(expression)).isNull();
	}
}
