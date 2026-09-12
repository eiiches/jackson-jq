package net.thisptr.jackson.jq.v2.core.internal.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public class NullModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {

	private static final NullModuleLoader<?> INSTANCE = new NullModuleLoader<>();

	@SuppressWarnings("unchecked")
	public static <JsonNode> NullModuleLoader<JsonNode> getInstance() {
		return (NullModuleLoader<JsonNode>) INSTANCE;
	}

	@Override
	public @Nullable Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
		return null;
	}

	@Override
	public Maybe<JsonNode> loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
		return Maybe.absent();
	}
}
