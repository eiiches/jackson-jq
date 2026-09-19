package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * A {@link Module} whose functions are implemented in Java and are ready to use as they are.
 * <p>
 * This is the module kind an extension ships: implement it, annotate it with
 * {@code @ModuleRegistration}, and register it for {@link java.util.ServiceLoader} discovery as a
 * {@link Module} service. It is also what the compiler produces from a {@link JqModule} once that
 * module's source has been compiled.
 * <p>
 * An implementation may also implement {@link JqModule}. Such a hybrid makes these Java functions
 * available to its jq source and exports them alongside the compiled jq functions.
 */
public non-sealed interface JavaModule extends Module {

	/**
	 * Returns all functions exported by this module, keyed by name and arity.
	 * <p>
	 * The returned map is an immutable, stable snapshot: repeated calls may return the same
	 * instance, and callers may cache the result rather than calling this method again.
	 *
	 * @return an unmodifiable map of function signatures to their factories
	 */
	Map<FunctionSignature, Function> getFunctions();
}
