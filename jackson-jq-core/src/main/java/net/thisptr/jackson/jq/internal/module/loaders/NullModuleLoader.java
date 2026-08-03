package net.thisptr.jackson.jq.internal.module.loaders;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

public class NullModuleLoader implements ModuleLoader {

	private static final NullModuleLoader INSTANCE = new NullModuleLoader();

	public static NullModuleLoader getInstance() {
		return INSTANCE;
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
