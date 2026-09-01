package net.thisptr.jackson.jq.v2.spi;

import java.util.List;

/**
 * A provider of raw, jq-language function definitions ({@link JqFunction}), as opposed to
 * {@link Function}, which provides Java-implemented functions.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}; see the
 * {@code net.thisptr.jackson.jq.v2.spi.annotations} package for the classpath, JPMS, and OSGi
 * registration requirements this entails. Implementations must be safe to hold as a singleton and
 * to call {@link #getJqFunctions()} from multiple threads concurrently.
 */
public interface JqLibrary {

	/**
	 * Returns the jq function definitions provided by this library.
	 * <p>
	 * The returned list is an immutable, stable snapshot: repeated calls may return the same
	 * instance, and callers may cache the result rather than calling this method again.
	 *
	 * @return the jq function definitions
	 */
	List<JqFunction> getJqFunctions();
}
