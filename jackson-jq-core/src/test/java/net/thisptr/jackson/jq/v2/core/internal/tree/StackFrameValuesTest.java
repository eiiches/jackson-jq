package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.utils.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StackFrameValuesTest {

	@Test
	void passesThroughAnExistingPathAndValue() {
		PathAndValue<String> pv = new PathAndValue<>(null, "value");

		assertSame(pv, StackFrameValues.<String>asPathAndValue(pv));
	}

	@Test
	void wrapsAPlainValueWithoutAPath() {
		PathAndValue<String> result = StackFrameValues.asPathAndValue("value");

		assertNotNull(result);
		assertNull(result.getPath());
		assertSame("value", result.getValue());
	}

	@Test
	void returnsNullForNullRawValue() {
		assertNull(StackFrameValues.asPathAndValue(null));
	}

	@Test
	void returnsNullForFunctionFactoryRawValue() {
		FunctionFactory factory = new FunctionFactory() {
			@Override
			public <N> Expression<N> createFunction(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version version) {
				throw new UnsupportedOperationException();
			}
		};

		assertNull(StackFrameValues.asPathAndValue(factory));
	}

	@Test
	void returnsNullForExpressionRawValue() {
		Expression<String> expression = (frame, in, path, output, requirePath) -> {
			throw new UnsupportedOperationException();
		};

		assertNull(StackFrameValues.asPathAndValue(expression));
	}
}
