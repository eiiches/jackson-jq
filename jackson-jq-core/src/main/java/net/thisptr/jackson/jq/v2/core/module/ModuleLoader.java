package net.thisptr.jackson.jq.v2.core.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

public interface ModuleLoader<JsonNode> {
	// import path as NAME
	@Nullable Module loadModule(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException;

	// import path as $NAME
	@Nullable JsonNode loadData(@Nullable Module caller, String path, @Nullable JsonNode metadata) throws JsonQueryException;
}
