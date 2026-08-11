package net.thisptr.jackson.jq.v2.spi.module;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
public interface ModuleLoader<JsonNode> {
	// import path as NAME
	Module<JsonNode> loadModule(Module<JsonNode> caller, String path, JsonNode metadata) throws JsonQueryException;

	// import path as $NAME
	JsonNode loadData(Module<JsonNode> caller, String path, JsonNode metadata) throws JsonQueryException;
}
