package net.thisptr.jackson.jq.v2.core.internal.function.utils;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * The modulemeta builtin's view of the calling query's module lookup scope.
 */
public interface ModuleMetaLookup<JsonNode> {
	JsonNode inspect(String path, JsonProvider<JsonNode> jsonProvider, Version jqVersion) throws JsonQueryException;
}
