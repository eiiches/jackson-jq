package net.thisptr.jackson.jq.v2.core.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class SimpleModule implements Module {
	private final Map<String, Function> functions = new HashMap<>();

	public void addFunction(String fname, int nargs, Function f) {
		addFunction(fname + "/" + nargs, f);
	}

	public void addFunction(String fnameAndNarg, Function f) {
		functions.put(fnameAndNarg, f);
	}

	public void addAllFunctions(Map<String, Function> functions) {
		this.functions.putAll(functions);
	}

	@Override
	public Function getFunction(String fname, int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
