package net.thisptr.jackson.jq.v2.core.internal.env;

import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.spi.type.Type;

/**
 * Concrete {@link Environment.Variable}. Register one with {@code EnvironmentBuilder.defineVariable}, not
 * directly.
 */
public record VariableImpl<JsonNode>(Type type, Supplier<JsonNode> supplier) implements Environment.Variable<JsonNode> {
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Supplier<JsonNode> getValue() {
		return supplier;
	}
}
