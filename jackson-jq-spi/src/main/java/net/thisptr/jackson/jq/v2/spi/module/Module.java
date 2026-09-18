package net.thisptr.jackson.jq.v2.spi.module;

/**
 * A jq module: a named unit that contributes functions to the queries that {@code import} it.
 * <p>
 * Every module implements one or both of the two sub-interfaces rather than this one:
 * <ul>
 * <li>{@link JavaModule} -- functions already implemented in Java. This is what a module author
 * ships and registers for {@link java.util.ServiceLoader} discovery; see the
 * {@code net.thisptr.jackson.jq.v2.spi.annotations} package for the classpath, JPMS, and OSGi
 * registration requirements that entails.</li>
 * <li>{@link JqModule} -- jq source that has not been compiled yet. This is what a module loader
 * returns when it finds a {@code .jq} file; the compiler resolves its imports and compiles it.</li>
 * </ul>
 * A module implementing both is a hybrid: its Java functions are available while its jq source
 * is compiled, and the resulting module exports both sets of functions. Defining the same exact
 * function signature in both is an error.
 * Implementations must be safe to hold as a singleton and to use from multiple threads
 * concurrently.
 * <p>
 * Nothing implements this interface directly: Java 17 and later seal it to the two sub-interfaces
 * via {@code permits}.
 */
public interface Module {

	/**
	 * Returns the metadata associated with this module, such as its {@code module {...};} directive
	 * and its declared dependencies.
	 * <p>
	 * A {@link JqModule} has not been parsed yet, so its source metadata is exposed by the compiled
	 * module the compiler produces from it. For a hybrid, source metadata is authoritative.
	 *
	 * @return the {@link ModuleMeta} of this module
	 */
	default ModuleMeta getModuleMeta() {
		return new ModuleMeta() {
		};
	}
}
