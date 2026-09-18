package net.thisptr.jackson.jq.v2.spi.module;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * A {@link Module} that is still jq source. A module loader returns one of these when an import
 * path resolves to something it can read but not run; the compiler parses it, resolves its own
 * imports, compiles it, and turns it into a {@link JavaModule}.
 * <p>
 * Loaders implement this themselves -- there is no shared implementation to extend -- because only
 * the loader knows where the module came from, which is what the two {@code relative*} methods and
 * {@link #equals} answer.
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
 *
 * @param <JsonNode> the JSON node type
 */
public interface JqModule<JsonNode> extends Module {

	/**
	 * Returns this module's jq source.
	 *
	 * @return the source text
	 */
	String getSourceCode();

	/**
	 * Resolves an import written inside this module against this module's own location -- what
	 * {@code import "d" as d {search: "./"}} means when it appears here.
	 * <p>
	 * Only this module can answer it: an import relative to it is relative to wherever its loader
	 * found it, which nothing else knows.
	 *
	 * @param importPath the import path, as written in the statement
	 * @param searchPath the {@code search} metadata value, as written
	 * @return the resolved module
	 * @throws JsonQueryException if the path does not resolve -- {@code ModuleNotFoundException} --
	 * or resolved but could not be read
	 */
	JqModule<JsonNode> relativeImport(String importPath, String searchPath) throws JsonQueryException;

	/**
	 * The same as {@link #relativeImport}, for an {@code import path as $NAME} statement.
	 *
	 * @param importPath the import path, as written in the statement
	 * @param searchPath the {@code search} metadata value, as written
	 * @return the resolved data
	 * @throws JsonQueryException if the path does not resolve -- {@code ModuleNotFoundException} --
	 * or resolved but could not be read
	 */
	JsonNode relativeData(String importPath, String searchPath) throws JsonQueryException;
}
