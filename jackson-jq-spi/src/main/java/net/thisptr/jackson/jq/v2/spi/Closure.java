package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

public class Closure<JsonNode> {
	private final Object[] variables;
	private final FunctionFactory[] functions;

	public Closure(int variableCount, int functionCount) {
		this.variables = new Object[variableCount];
		this.functions = new FunctionFactory[functionCount];
	}

	public Closure(Object[] variables, FunctionFactory[] functions) {
		this.variables = variables;
		this.functions = functions;
	}

	public @Nullable Object getVariable(int slot) {
		if (slot < 0 || slot >= variables.length)
			return null;
		return variables[slot];
	}

	public void setVariable(int slot, @Nullable Object value) {
		if (slot >= 0 && slot < variables.length) {
			variables[slot] = value;
		}
	}

	public @Nullable FunctionFactory getFunctionFactory(int slot) {
		if (slot < 0 || slot >= functions.length)
			return null;
		return functions[slot];
	}

	public void setFunctionFactory(int slot, @Nullable FunctionFactory factory) {
		if (slot >= 0 && slot < functions.length) {
			functions[slot] = factory;
		}
	}

	public int getVariableCount() {
		return variables.length;
	}

	public int getFunctionCount() {
		return functions.length;
	}
}
