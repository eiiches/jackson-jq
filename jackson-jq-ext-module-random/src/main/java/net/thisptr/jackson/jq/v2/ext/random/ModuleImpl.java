package net.thisptr.jackson.jq.v2.ext.random;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.random.functions.RandomFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleRegistration;

@SuppressWarnings("rawtypes")
@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/random")
public class ModuleImpl<JsonNode> implements Module<JsonNode> {
	private final Map<String, Function<JsonNode>> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("random/0", new RandomFunction<>());
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
