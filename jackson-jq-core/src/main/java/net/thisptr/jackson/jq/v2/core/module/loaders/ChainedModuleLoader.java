package net.thisptr.jackson.jq.v2.core.module.loaders;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class ChainedModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final ModuleLoader<JsonNode>[] loaders;

	@SafeVarargs
	public ChainedModuleLoader(final ModuleLoader<JsonNode>... loaders) {
		this.loaders = loaders;
	}

	@Override
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader<JsonNode> loader : loaders) {
			final Module module = loader.loadModule(caller, path, metadata);
			if (module != null)
				return module;
		}
		return null;
	}

	@Override
	public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader<JsonNode> loader : loaders) {
			final JsonNode data = loader.loadData(caller, path, metadata);
			if (data != null)
				return data;
		}
		return null;
	}
}
