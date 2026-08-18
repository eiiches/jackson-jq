package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Factory for creating executable {@link Expression} instances.
 * <p>
 * A {@code FunctionFactory} represents a compiled function expression bound to its lexical environment.
 * Any captured lexical state (such as a {@code Closure}) is encapsulated internally by the specific
 * {@code FunctionFactory} implementation rather than supplied dynamically at invocation time by callers.
 */
@FunctionalInterface
public interface FunctionFactory {
	<JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version);
}
