package net.thisptr.jackson.jq.v2.spi.internal.module.loaders;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;

public class NullModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

	private static final NullModuleLoader<?> INSTANCE = new NullModuleLoader<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> NullModuleLoader<JsonNode> getInstance() {
		return (NullModuleLoader<JsonNode>) INSTANCE;
	}

	@Override
	public @Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
		return null;
	}

	@Override
	public @Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) {
		return null;
	}
}
