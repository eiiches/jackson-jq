package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class SimpleModule implements Module {
	private final Map<String, FunctionFactory> functions = new HashMap<>();

	public void addFunction(String fname, int nargs, FunctionFactory f) {
		addFunction(fname + "/" + nargs, f);
	}

	public void addFunction(String fnameAndNarg, FunctionFactory f) {
		functions.put(fnameAndNarg, f);
	}

	public void addAllFunctions(Map<String, FunctionFactory> functions) {
		this.functions.putAll(functions);
	}

	@Override
	public @Nullable FunctionFactory getFunction(String fname, int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, FunctionFactory> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
