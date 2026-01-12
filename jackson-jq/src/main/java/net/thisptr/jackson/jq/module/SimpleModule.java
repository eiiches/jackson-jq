package net.thisptr.jackson.jq.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.annotations.Experimental;

@Experimental
public class SimpleModule<JsonNode> implements Module<JsonNode> {
	private final Map<String, Function<JsonNode>> functions = new HashMap<>();

	public void addFunction(final String fname, final int nargs, final Function<JsonNode> f) {
		addFunction(fname + "/" + nargs, f);
	}

	public void addFunction(final String fnameAndNarg, final Function<JsonNode> f) {
		functions.put(fnameAndNarg, f);
	}

	public void addAllFunctions(final Map<String, Function<JsonNode>> functions) {
		this.functions.putAll(functions);
	}

	@Override
	public Function<JsonNode> getFunction(final String fname, final int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function<JsonNode>> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
