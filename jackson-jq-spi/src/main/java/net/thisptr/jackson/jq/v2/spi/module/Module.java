package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Represents a jq module that provides functions and metadata.
 */
public interface Module {

	/**
	 * Returns all functions exported by this module, keyed by name and arity.
	 *
	 * @return an unmodifiable map of function signatures to their factories
	 */
	Map<FunctionSignature, Function> getFunctions();

	/**
	 * Returns the metadata associated with this module, such as dependencies,
	 * definitions, and module-level metadata.
	 *
	 * @return the {@link ModuleMeta} of this module
	 */
	ModuleMeta getModuleMeta();
}
