package net.thisptr.jackson.jq.module.loaders;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

public class ChainedModuleLoader implements ModuleLoader {
	private final ModuleLoader[] loaders;

	public ChainedModuleLoader(final ModuleLoader... loaders) {
		this.loaders = loaders;
	}

	@Override
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader loader : loaders) {
			final Module module = loader.loadModule(caller, path, metadata);
			if (module != null)
				return module;
		}
		return null;
	}

	@Override
	public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader loader : loaders) {
			final JsonNode data = loader.loadData(caller, path, metadata);
			if (data != null)
				return data;
		}
		return null;
	}
}
