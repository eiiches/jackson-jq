package net.thisptr.jackson.jq.v2.core.function;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * Contextual information about a function being evaluated by a {@link FunctionFilter}.
 *
 * <p>Contains the function's {@link FunctionSignature}, the {@link FunctionOrigin} describing how
 * the function entered the system, and the locator identifying the specific module or loader it
 * came from.</p>
 *
 * <p>For functions loaded via a {@link FunctionLoader}, the locator is the fully-qualified class
 * name of the loader. For built-in jq functions this is
 * {@code "net.thisptr.jackson.jq.v2.core.function.loaders.BuiltinFunctionLoader"}.</p>
 */
public final class FunctionContext {
	private final FunctionSignature signature;
	private final FunctionOrigin origin;
	private final String locator;

	private FunctionContext(FunctionSignature signature, FunctionOrigin origin, String locator) {
		this.signature = Objects.requireNonNull(signature, "signature");
		this.origin = Objects.requireNonNull(origin, "origin");
		this.locator = Objects.requireNonNull(locator, "locator");
	}

	/**
	 * Creates a new {@link FunctionContext} for the given signature, origin and locator.
	 *
	 * @param signature the signature of the function
	 * @param origin the mechanism by which the function entered the system
	 * @param locator the string identifying the specific module or loader within the origin
	 * @return the function context
	 */
	public static FunctionContext of(FunctionSignature signature, FunctionOrigin origin, String locator) {
		return new FunctionContext(signature, origin, locator);
	}

	/**
	 * Returns the signature of the function.
	 *
	 * @return the function signature
	 */
	public FunctionSignature getSignature() {
		return signature;
	}

	/**
	 * Returns the origin describing how the function entered the system.
	 *
	 * @return the function origin
	 */
	public FunctionOrigin getOrigin() {
		return origin;
	}

	/**
	 * Returns the string identifying the specific module or loader within its origin.
	 * For {@link FunctionOrigin#PATH_IMPORT} this is the module path (e.g. {@code "jackson/uuid"}).
	 * For {@link FunctionOrigin#ENV_IMPORT} this is the alias (e.g. {@code "uuid_alias"}).
	 * For {@link FunctionOrigin#ENV_INCLUDE} this is the fully-qualified class name of the module.
	 * For {@link FunctionOrigin#FUNCTION_LOADER} this is the fully-qualified class name of the loader.
	 *
	 * @return the locator string
	 */
	public String getLocator() {
		return locator;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof FunctionContext that))
			return false;
		return Objects.equals(signature, that.signature)
				&& origin == that.origin
				&& Objects.equals(locator, that.locator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signature, origin, locator);
	}

	@Override
	public String toString() {
		return origin + ":" + locator + ":" + signature;
	}
}
