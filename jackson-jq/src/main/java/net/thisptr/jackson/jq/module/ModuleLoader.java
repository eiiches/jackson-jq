package net.thisptr.jackson.jq.module;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface ModuleLoader {
	// import path as NAME
	Module loadModule(Module caller, String path, JsonNode metadata) throws JsonQueryException;

	// import path as $NAME
	JsonNode loadData(Module caller, String path, JsonNode metadata) throws JsonQueryException;
}
