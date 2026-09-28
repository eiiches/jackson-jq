package net.thisptr.jackson.jq.v2.core.internal.function.utils;

import net.thisptr.jackson.jq.v2.spi.BindContext;

/**
 * Core-only bind context for builtins that inspect modules from their call site.
 */
public interface ModuleMetaBindContext<JsonNode> extends BindContext<JsonNode> {
	ModuleMetaLookup<JsonNode> getModuleMetaLookup();
}
