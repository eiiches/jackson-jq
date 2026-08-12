package net.thisptr.jackson.jq.v2.spi.module;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
public interface ModuleLoader<JsonNode> {
	// import path as NAME
	Module loadModule(Module caller, String path, JsonNode metadata) throws JsonQueryException;

	// import path as $NAME
	JsonNode loadData(Module caller, String path, JsonNode metadata) throws JsonQueryException;
}
