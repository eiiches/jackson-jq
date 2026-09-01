package net.thisptr.jackson.jq.v2.core.module.loaders;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class ChainedModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final ModuleLoader<JsonNode>[] loaders;

	@SafeVarargs
	public ChainedModuleLoader(ModuleLoader<JsonNode>... loaders) {
		this.loaders = loaders;
	}

	@Override
	public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
		for (ModuleLoader<JsonNode> loader : loaders) {
			Module module = loader.loadModule(caller, path, metadata);
			if (module != null)
				return module;
		}
		return null;
	}

	@Override
	public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException {
		for (ModuleLoader<JsonNode> loader : loaders) {
			JsonNode data = loader.loadData(caller, path, metadata);
			if (data != null)
				return data;
		}
		return null;
	}
}
