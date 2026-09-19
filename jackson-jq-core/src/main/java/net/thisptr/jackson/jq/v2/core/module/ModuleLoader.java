package net.thisptr.jackson.jq.v2.core.module;

import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Resolves the paths appearing in {@code import} and {@code include} statements.
 * <p>
 * A loader resolves; it does not compile and it does not cache. Handing back a {@link JqModule} --
 * a name and its source -- is the whole job for a module written in jq: the compiler parses it,
 * resolves that module's own imports through the environment's loaders, compiles it, and remembers
 * the result for the rest of the compilation. A loader that serves modules already implemented in
 * Java hands back a {@link JavaModule}, which the compiler uses as it is. A module may implement
 * both: the compiler makes its Java functions available to its jq source and exports both sets.
 * <p>
 * A loader that cannot resolve a path throws {@link ModuleNotFoundException}; a loader that
 * resolved it but then failed to read what it found throws some other {@link JsonQueryException}.
 * The compiler depends on the difference: the former moves on to the next loader, the latter aborts
 * the search rather than being masked by a later loader's answer or by a misleading "module not
 * found".
 *
 * @param <JsonNode> the JSON node type
 */
public interface ModuleLoader<JsonNode> {

	/**
	 * Resolves the module an {@code import path as NAME} (or {@code include path}) statement refers
	 * to.
	 * <p>
	 * {@code path} is the import path as written, always relative. An import carrying a
	 * {@code {search: ...}} override never reaches a loader at all -- the importing module resolves
	 * that one itself, through {@link JqModule#relativeImport} -- which is why a loader is never
	 * told who is importing.
	 *
	 * @param path the import path to resolve
	 * @param metadata the import statement's metadata object, or absent if it has none
	 * @return the resolved module
	 * @throws ModuleNotFoundException if this loader cannot resolve {@code path}
	 * @throws JsonQueryException if the module was found but could not be read
	 */
	Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException;

	/**
	 * Resolves the data an {@code import path as $NAME} statement refers to.
	 *
	 * @param path the import path to resolve, on the same terms as {@link #loadModule}
	 * @param metadata the import statement's metadata object, or absent if it has none
	 * @return the resolved data
	 * @throws ModuleNotFoundException if this loader cannot resolve {@code path}
	 * @throws JsonQueryException if the data was found but could not be read
	 */
	JsonNode loadData(String path, Maybe<JsonNode> metadata) throws JsonQueryException;
}
