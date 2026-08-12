package net.thisptr.jackson.jq.v2.spi.internal.module.loaders;

import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class NullModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

	private static final NullModuleLoader<Object> INSTANCE = new NullModuleLoader<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> NullModuleLoader<JsonNode> getInstance() {
		return (NullModuleLoader<JsonNode>) INSTANCE;
	}

	@Override
	public Module loadModule(Module caller, String path, JsonNode metadata) {
		return null;
	}

	@Override
	public JsonNode loadData(Module caller, String path, JsonNode metadata) {
		return null;
	}
}
