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
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) {
		return null;
	}

	@Override
	public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) {
		return null;
	}
}
