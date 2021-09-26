package net.thisptr.jackson.jq.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.Function;

public class SimpleModule implements Module {
	private final Map<String, Function> functions = new HashMap<>();

	public void addFunction(final String fname, final int nargs, final Function f) {
		addFunction(fname + "/" + nargs, f);
	}

	public void addFunction(final String fnameAndNarg, final Function f) {
		functions.put(fnameAndNarg, f);
	}

	public void addAllFunctions(final Map<String, Function> functions) {
		this.functions.putAll(functions);
	}

	@Override
	public Function getFunction(final String fname, final int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
