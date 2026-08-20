package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class SimpleModule implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public void addFunction(String fname, int nargs, Function f) {
		addFunction(FunctionSignature.of(fname, nargs), f);
	}

	public void addFunction(FunctionSignature key, Function f) {
		functions.put(key, f);
	}

	public void addAllFunctions(Map<FunctionSignature, Function> functions) {
		this.functions.putAll(functions);
	}

	@Override
	public @Nullable Function resolveFunction(String fname, int nargs) {
		return functions.get(FunctionSignature.of(fname, nargs));
	}

	@Override
	public Map<FunctionSignature, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
