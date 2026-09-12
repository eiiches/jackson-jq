package net.thisptr.jackson.jq.v2.core.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Resolves the paths appearing in {@code import} and {@code include} statements.
 * <p>
 * A loader that cannot resolve a path throws {@link ModuleNotFoundException}; a loader that
 * resolved it but then failed to read or compile what it found throws some other
 * {@link JsonQueryException}. {@code ChainedModuleLoader} depends on the difference: the former
 * moves on to the next loader in the chain, the latter aborts the chain.
 *
 * @param <JsonNode> the JSON node type
 */
public interface ModuleLoader<JsonNode> {

	/**
	 * Loads the module an {@code import path as NAME} (or {@code include path}) statement refers to.
	 *
	 * @param caller the module containing the import statement, or {@code null} for a top-level query
	 * @param path the import path, as written in the statement
	 * @param metadata the import statement's metadata object, or absent if it has none
	 * @return the resolved module
	 * @throws ModuleNotFoundException if this loader cannot resolve {@code path}
	 * @throws JsonQueryException if the module was found but could not be loaded
	 */
	Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) throws JsonQueryException;

	/**
	 * Loads the data an {@code import path as $NAME} statement refers to.
	 *
	 * @param caller the module containing the import statement, or {@code null} for a top-level query
	 * @param path the import path, as written in the statement
	 * @param metadata the import statement's metadata object, or absent if it has none
	 * @return the resolved data
	 * @throws ModuleNotFoundException if this loader cannot resolve {@code path}
	 * @throws JsonQueryException if the data was found but could not be loaded
	 */
	JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) throws JsonQueryException;
}
