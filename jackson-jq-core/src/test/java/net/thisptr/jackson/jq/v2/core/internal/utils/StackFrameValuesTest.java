package net.thisptr.jackson.jq.v2.core.internal.utils;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StackFrameValuesTest {

	@Test
	void passesThroughAnExistingPathAndValue() {
		PathAndValue<String> pv = new PathAndValue<>(UntrackedPath.getInstance(), "value");

		assertSame(pv, StackFrameValues.<String>asPathAndValue(pv));
	}

	@Test
	void wrapsAPlainValueWithoutAPath() {
		PathAndValue<String> result = StackFrameValues.asPathAndValue("value");

		assertNotNull(result);
		assertSame(UntrackedPath.getInstance(), result.getPath());
		assertSame("value", result.getValue());
	}

	@Test
	void returnsNullForNullRawValue() {
		assertNull(StackFrameValues.asPathAndValue(null));
	}

	@Test
	void roundTripsAPlainValueThroughASlot() {
		PathAndValue<String> result = StackFrameValues.asPathAndValue(StackFrameValues.toSlot("value"));

		assertNotNull(result);
		assertSame(UntrackedPath.getInstance(), result.getPath());
		assertSame("value", result.getValue());
	}

	// A provider representing JSON null as Java null binds exactly this. It must not be stored as a
	// bare null, which a slot reads as an unset variable. NullAway cannot express such a value, which
	// is the whole reason the slot encoding exists.
	@SuppressWarnings("NullAway")
	@Test
	void roundTripsAJavaNullValueThroughASlotAsAValue() {
		Object slot = StackFrameValues.toSlot((String) null);
		assertNotNull(slot);

		PathAndValue<String> result = StackFrameValues.asPathAndValue(slot);

		assertNotNull(result);
		assertSame(UntrackedPath.getInstance(), result.getPath());
		assertNull(result.getValue());
	}

	@Test
	void returnsNullForFunctionRawValue() {
		Function factory = new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, Version version) {
				throw new UnsupportedOperationException();
			}
		};

		assertNull(StackFrameValues.asPathAndValue(factory));
	}

	@Test
	void returnsNullForExpressionRawValue() {
		Expression<Object, String> expression = (frame, in, path, output) -> {
			throw new UnsupportedOperationException();
		};

		assertNull(StackFrameValues.asPathAndValue(expression));
	}
}
