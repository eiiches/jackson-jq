package net.thisptr.jackson.jq.v2.ext.time;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.time.functions.StrFTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.StrPTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.TimestampFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleRegistration;

@SuppressWarnings("rawtypes")
@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/time")
public class ModuleImpl<JsonNode> implements Module<JsonNode> {
	private final Map<String, Function<JsonNode>> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("strftime/1", new StrFTimeFunction<>());
		functions.put("strftime/2", new StrFTimeFunction<>());
		functions.put("strptime/1", new StrPTimeFunction<>());
		functions.put("strptime/2", new StrPTimeFunction<>());
		functions.put("timestamp/0", new TimestampFunction<>());
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
