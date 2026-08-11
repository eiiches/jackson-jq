package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid35Function;
import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid4Function;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleRegistration;

@SuppressWarnings("rawtypes")
@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uuid")
public class ModuleImpl<JsonNode> implements Module<JsonNode> {
	private final Map<String, Function<JsonNode>> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("uuid4/0", new Uuid4Function<>());
		functions.put("uuid3/1", new Uuid35Function<>(3));
		functions.put("uuid5/1", new Uuid35Function<>(5));
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
