package net.thisptr.jackson.jq.v2.core.function;

import java.util.Set;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Decides which functions are visible to an environment.
 *
 * <p>A filter is a predicate on the {@link FunctionContext} (comprising the {@code (name, arity)} signature,
 * the {@link FunctionOrigin} describing the registration mechanism, and the locator identifying the specific
 * module or loader) of a function and applies equally to Java and jq functions, regardless of where they
 * come from (built-ins, {@link FunctionLoader}s, or modules). Functions registered explicitly on the
 * environment builder are never filtered.</p>
 *
 * <p>A filter is also applied to functions that other built-in or module functions depend on. Excluding such a
 * function makes its dependents fail, so choosing what to exclude is the responsibility of the caller.</p>
 *
 * <p>Signatures in the context passed to {@link #accept(FunctionContext)} may be variadic, see
 * {@link FunctionSignature#isVariadic()}. For functions sourced from a {@link FunctionLoader} (including
 * built-ins), the origin is {@link FunctionOrigin#FUNCTION_LOADER} and the locator is the fully-qualified
 * class name of the loader. Implementations must be thread-safe and must return the same answer for the
 * same context.</p>
 */
@FunctionalInterface
public interface FunctionFilter {
	/**
	 * Tells whether the function described by the given context is made available.
	 *
	 * @param context the function context containing signature and module path
	 * @return {@code true} if the function is accepted, {@code false} if it is excluded
	 */
	boolean accept(FunctionContext context);

	/**
	 * Returns a filter that rejects exactly the given signatures regardless of module path.
	 *
	 * <p>A signature matches only if both the name and the arity are equal. A variadic signature (for example
	 * {@code FunctionSignature.valueOf("custom/*")}) matches only variadic functions with that name, not fixed-arity
	 * ones. Use {@link #denyNames(Set)} to reject a name with any arity.</p>
	 *
	 * @param signatures the signatures to reject
	 * @return the filter
	 */
	static FunctionFilter denySignatures(Set<FunctionSignature> signatures) {
		Set<FunctionSignature> denied = Set.copyOf(signatures);
		return context -> !denied.contains(context.getSignature());
	}

	/**
	 * Returns a filter that rejects every function with one of the given names, regardless of arity
	 * (including variadic functions) or module path.
	 *
	 * @param names the function names to reject
	 * @return the filter
	 */
	static FunctionFilter denyNames(Set<String> names) {
		Set<String> denied = Set.copyOf(names);
		return context -> !denied.contains(context.getSignature().name());
	}

	/**
	 * Returns a filter that accepts every function. This is the default when no filter is set.
	 *
	 * @return the filter
	 */
	static FunctionFilter acceptAll() {
		return AcceptAll.INSTANCE;
	}
}
