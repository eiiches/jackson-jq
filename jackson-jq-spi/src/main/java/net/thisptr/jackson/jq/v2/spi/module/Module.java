package net.thisptr.jackson.jq.v2.spi.module;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Function;

/**
 * Represents a jq module that provides functions and metadata.
 */
public interface Module {

	/**
	 * Resolves an exported function by name and arity.
	 *
	 * @param fname the function name
	 * @param nargs the number of arguments (arity)
	 * @return the resolved {@link Function}, or {@code null} if not found in this module
	 */
	@Nullable Function resolveFunction(String fname, int nargs);

	/**
	 * Returns the metadata associated with this module, such as dependencies,
	 * definitions, and module-level metadata.
	 *
	 * @return the {@link ModuleMeta} of this module
	 */
	ModuleMeta getModuleMeta();
}
