package net.thisptr.jackson.jq.v2.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;

/**
 * Per-invocation overrides for variables and functions registered in an {@link Environment}.
 */
public final class JsonQueryBindings<JsonNode> {
	private static final JsonQueryBindings<?> EMPTY = new JsonQueryBindings<>(Collections.emptyMap(), Collections.emptyMap());

	private final Map<String, JsonNode> variables;
	private final Map<FunctionNameAndArity, FunctionFactory> functionFactories;

	private JsonQueryBindings(Map<String, JsonNode> variables, Map<FunctionNameAndArity, FunctionFactory> functionFactories) {
		this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
		this.functionFactories = Collections.unmodifiableMap(new HashMap<>(functionFactories));
	}

	@SuppressWarnings("unchecked")
	public static <JsonNode> JsonQueryBindings<JsonNode> empty() {
		return (JsonQueryBindings<JsonNode>) EMPTY;
	}

	public static <JsonNode> Builder<JsonNode> builder() {
		return new Builder<>();
	}

	public Map<String, JsonNode> variables() {
		return variables;
	}

	public Map<FunctionNameAndArity, FunctionFactory> functionFactories() {
		return functionFactories;
	}

	public static final class Builder<JsonNode> {
		private final Map<String, JsonNode> variables = new HashMap<>();
		private final Map<FunctionNameAndArity, FunctionFactory> functionFactories = new HashMap<>();

		public Builder<JsonNode> addVariable(String name, JsonNode value) {
			variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
			return this;
		}

		public Builder<JsonNode> addFunctionFactory(FunctionNameAndArity nameAndArity, FunctionFactory functionFactory) {
			functionFactories.put(Objects.requireNonNull(nameAndArity, "nameAndArity"), Objects.requireNonNull(functionFactory, "functionFactory"));
			return this;
		}

		public JsonQueryBindings<JsonNode> build() {
			if (variables.isEmpty() && functionFactories.isEmpty())
				return JsonQueryBindings.empty();
			return new JsonQueryBindings<>(variables, functionFactories);
		}
	}
}
