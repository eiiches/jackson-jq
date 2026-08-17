package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Factory for creating executable {@link Function} instances.
 * <p>
 * A {@code FunctionFactory} represents a compiled function instance bound to its lexical environment.
 * Any captured lexical state (such as a {@code Closure}) is encapsulated internally by the specific
 * {@code FunctionFactory} implementation rather than supplied dynamically at invocation time by callers.
 */
@FunctionalInterface
public interface FunctionFactory {
	<JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version);
}
