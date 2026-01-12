package net.thisptr.jackson.jq.module.loaders;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.annotations.Experimental;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

@Experimental
public class ChainedModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final ModuleLoader<JsonNode>[] loaders;

	@SafeVarargs
	public ChainedModuleLoader(final ModuleLoader<JsonNode>... loaders) {
		this.loaders = loaders;
	}

	@Override
	public Module<JsonNode> loadModule(final Module<JsonNode> caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader<JsonNode> loader : loaders) {
			final Module<JsonNode> module = loader.loadModule(caller, path, metadata);
			if (module != null)
				return module;
		}
		return null;
	}

	@Override
	public JsonNode loadData(final Module<JsonNode> caller, final String path, final JsonNode metadata) throws JsonQueryException {
		for (final ModuleLoader<JsonNode> loader : loaders) {
			final JsonNode data = loader.loadData(caller, path, metadata);
			if (data != null)
				return data;
		}
		return null;
	}
}
