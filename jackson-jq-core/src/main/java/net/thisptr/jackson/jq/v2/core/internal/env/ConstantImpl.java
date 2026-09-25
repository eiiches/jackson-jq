package net.thisptr.jackson.jq.v2.core.internal.env;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.spi.type.Type;

/**
 * Concrete {@link Environment.Constant}. Register one with {@code EnvironmentBuilder.defineConstant}, not
 * directly.
 */
public record ConstantImpl<JsonNode>(Type type, JsonNode value) implements Environment.Constant<JsonNode> {
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public JsonNode getConstantValue() {
		return value;
	}
}
