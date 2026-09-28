package net.thisptr.jackson.jq.v2.spi.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;

/**
 * A {@link Module} that is still jq source. A module loader returns one of these when an import
 * path resolves to something it can read but not run; the compiler parses it, resolves its own
 * imports, compiles it, and turns it into a {@link JavaModule}.
 * <p>
 * An implementation may also implement {@link JavaModule}. The compiler makes a hybrid's Java
 * functions available to this source and exports both sets after compilation.
 * <p>
 * A jq module works with any JSON provider. Its source and module imports are provider independent;
 * data imports are constructed with the provider supplied to {@link #loadData}.
 * <p>
 * Loaders and in-memory modules implement this themselves -- there is no shared implementation to
 * extend. The two {@code load*} methods resolve imports against the module's own location or
 * contents, and {@link #equals} identifies the same module across repeated imports.
 * <p>
 * <b>Implementations must implement {@link #equals} and {@link Object#hashCode} to mean "the same
 * module".</b> Two instances holding the same module file are equal even when they were reached by
 * different import paths, and two different modules never are. The compiler compiles each distinct
 * module once per compilation and reports a cycle when it re-enters one that is still being
 * compiled, so both rest on this: an implementation that inherits identity equality is compiled
 * again for every import, and a cycle through it recurses until the stack gives out.
 * <p>
 * {@link Object#toString} is worth overriding too: it is what names the module when a circular
 * import is reported.
 */
public non-sealed interface JqModule extends Module {
	/**
	 * Returns this module's jq source.
	 *
	 * @return the source text
	 */
	String getSourceCode();

	/**
	 * Resolves a module import written inside this module's source. An ordinary import passes
	 * {@code null} for {@code searchPath}; an import with a {@code search} override passes its value.
	 * <p>
	 * A module found here is available only while compiling this module, before the importing
	 * environment's registrations and loaders. A {@link ModuleNotFoundException} from an ordinary
	 * import lets resolution continue there. A relative import must be resolved here.
	 *
	 * @param importPath the import path, as written in the statement
	 * @param searchPath the {@code search} metadata value, or {@code null} for an ordinary import
	 * @return the resolved jq or Java module
	 * @throws ModuleNotFoundException if this module cannot resolve the path
	 * @throws JsonQueryException if it found the module but could not read it
	 */
	default Module loadModule(String importPath, @Nullable String searchPath) throws JsonQueryException {
		throw new ModuleNotFoundException(importPath);
	}

	/**
	 * Resolves a data import written inside this module's source, on the same terms as
	 * {@link #loadModule(String, String)}. Construct the result with {@code jsonProvider} so it
	 * belongs to the environment compiling this module.
	 *
	 * @param <JsonNode> the active environment's JSON node type
	 * @param importPath the import path, as written in the statement
	 * @param searchPath the {@code search} metadata value, or {@code null} for an ordinary import
	 * @param jsonProvider the active environment's JSON provider, used to construct the data
	 * @return the resolved data in the active environment's node type
	 * @throws ModuleNotFoundException if this module cannot resolve the path
	 * @throws JsonQueryException if it found the data but could not read it
	 */
	default <JsonNode> JsonNode loadData(String importPath, @Nullable String searchPath, JsonProvider<JsonNode> jsonProvider) throws JsonQueryException {
		throw new ModuleNotFoundException(importPath);
	}
}
