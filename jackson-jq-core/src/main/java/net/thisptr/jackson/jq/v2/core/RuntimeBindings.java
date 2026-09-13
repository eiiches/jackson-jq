package net.thisptr.jackson.jq.v2.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Per-invocation overrides for variables and functions registered in an {@link Environment}.
 */
public final class RuntimeBindings<JsonNode> {
	private static final RuntimeBindings<?> EMPTY = new RuntimeBindings<>(Collections.emptyMap(), Collections.emptyMap());

	private final Map<String, Supplier<JsonNode>> variables;
	private final Map<FunctionSignature, Function> functions;

	private RuntimeBindings(Map<String, Supplier<JsonNode>> variables, Map<FunctionSignature, Function> functions) {
		this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
		this.functions = Collections.unmodifiableMap(new HashMap<>(functions));
	}

	// Package-private: JsonQuery's no-bindings overloads need an instance to pass to apply(), but
	// callers never do -- they use the apply() overloads that take no bindings.
	@SuppressWarnings("unchecked")
	static <JsonNode> RuntimeBindings<JsonNode> getDefaultInstance() {
		return (RuntimeBindings<JsonNode>) EMPTY;
	}

	public static <JsonNode> Builder<JsonNode> newBuilder() {
		return new Builder<>();
	}

	public Map<String, Supplier<JsonNode>> getVariables() {
		return variables;
	}

	public Map<FunctionSignature, Function> getFunctions() {
		return functions;
	}

	public static final class Builder<JsonNode> {
		private final Map<String, Supplier<JsonNode>> variables = new HashMap<>();
		private final Map<FunctionSignature, Function> functions = new HashMap<>();

		public Builder<JsonNode> setVariable(String name, JsonNode value) {
			Objects.requireNonNull(value, "value");
			return setVariable(name, () -> value);
		}

		/**
		 * Adds a variable whose supplier is evaluated whenever the variable is referenced.
		 */
		public Builder<JsonNode> setVariable(String name, Supplier<JsonNode> supplier) {
			variables.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(supplier, "supplier"));
			return this;
		}

		public Builder<JsonNode> setFunction(FunctionSignature nameAndArity, Function function) {
			functions.put(Objects.requireNonNull(nameAndArity, "nameAndArity"), Objects.requireNonNull(function, "function"));
			return this;
		}

		public RuntimeBindings<JsonNode> build() {
			if (variables.isEmpty() && functions.isEmpty())
				return RuntimeBindings.getDefaultInstance();
			return new RuntimeBindings<>(variables, functions);
		}
	}
}
