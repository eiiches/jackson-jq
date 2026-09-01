package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Represents a jq module that provides functions and metadata.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}, the same as
 * {@link net.thisptr.jackson.jq.v2.spi.JqLibrary}; see the
 * {@code net.thisptr.jackson.jq.v2.spi.annotations} package for the classpath, JPMS, and OSGi
 * registration requirements this entails. Implementations must be safe to hold as a singleton and
 * to call {@link #getFunctions()}/{@link #getModuleMeta()} from multiple threads concurrently.
 */
public interface Module {

	/**
	 * Returns all functions exported by this module, keyed by name and arity.
	 * <p>
	 * The returned map is an immutable, stable snapshot: repeated calls may return the same
	 * instance, and callers may cache the result rather than calling this method again.
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
	default ModuleMeta getModuleMeta() {
		return new ModuleMeta() {
		};
	}
}
