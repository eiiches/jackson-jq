package net.thisptr.jackson.jq.v2.core.internal.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class NullModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

	private static final NullModuleLoader<?> INSTANCE = new NullModuleLoader<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> NullModuleLoader<JsonNode> getInstance() {
		return (NullModuleLoader<JsonNode>) INSTANCE;
	}

	@Override
	public Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
		throw new ModuleNotFoundException(path);
	}

	@Override
	public JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
		throw new ModuleNotFoundException(path);
	}
}
