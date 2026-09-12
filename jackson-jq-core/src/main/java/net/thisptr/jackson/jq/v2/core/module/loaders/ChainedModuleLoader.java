package net.thisptr.jackson.jq.v2.core.module.loaders;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Asks each loader in turn, and answers with the first one that resolves the path.
 * <p>
 * Only {@link ModuleNotFoundException} moves the chain along. A loader that did resolve the path
 * and then failed -- a module file with a syntax error, say -- throws a plain
 * {@link JsonQueryException}, and that aborts the chain rather than being masked by a later
 * loader's answer or by a misleading "module not found".
 */
public class ChainedModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final ModuleLoader<JsonNode>[] loaders;

	@SafeVarargs
	public ChainedModuleLoader(ModuleLoader<JsonNode>... loaders) {
		this.loaders = loaders;
	}

	@Override
	public Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		for (ModuleLoader<JsonNode> loader : loaders) {
			try {
				return loader.loadModule(caller, path, metadata);
			} catch (ModuleNotFoundException e) {
				/* this loader doesn't have it; try the next one */
			}
		}
		throw new ModuleNotFoundException(path);
	}

	@Override
	public JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		for (ModuleLoader<JsonNode> loader : loaders) {
			try {
				return loader.loadData(caller, path, metadata);
			} catch (ModuleNotFoundException e) {
				/* this loader doesn't have it; try the next one */
			}
		}
		throw new ModuleNotFoundException(path);
	}
}
