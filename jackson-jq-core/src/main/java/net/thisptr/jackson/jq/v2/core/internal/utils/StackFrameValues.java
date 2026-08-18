package net.thisptr.jackson.jq.v2.core.internal.utils;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;

public final class StackFrameValues {
	private StackFrameValues() {
	}

	@SuppressWarnings("unchecked")
	public static @Nullable <JsonNode> PathAndValue<JsonNode> asPathAndValue(@Nullable Object raw) {
		if (raw instanceof PathAndValue) {
			return (PathAndValue<JsonNode>) raw;
		} else if (raw != null && !(raw instanceof FunctionFactory) && !(raw instanceof Expression)) {
			return new PathAndValue<>(null, (JsonNode) raw);
		}
		return null;
	}
}
