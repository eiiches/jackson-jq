package net.thisptr.jackson.jq.module;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.annotations.Experimental;

@Experimental
public interface ModuleLoader<JsonNode> {
	// import path as NAME
	Module<JsonNode> loadModule(Module<JsonNode> caller, String path, JsonNode metadata) throws JsonQueryException;

	// import path as $NAME
	JsonNode loadData(Module<JsonNode> caller, String path, JsonNode metadata) throws JsonQueryException;
}
