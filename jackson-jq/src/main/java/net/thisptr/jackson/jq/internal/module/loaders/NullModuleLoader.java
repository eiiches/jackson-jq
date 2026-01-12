package net.thisptr.jackson.jq.internal.module.loaders;

import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

public class NullModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

	private static final NullModuleLoader<Object> INSTANCE = new NullModuleLoader<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> NullModuleLoader<JsonNode> getInstance() {
		return (NullModuleLoader<JsonNode>) INSTANCE;
	}

	@Override
	public Module<JsonNode> loadModule(final Module<JsonNode> caller, final String path, final JsonNode metadata) {
		return null;
	}

	@Override
	public JsonNode loadData(final Module<JsonNode> caller, final String path, final JsonNode metadata) {
		return null;
	}
}
