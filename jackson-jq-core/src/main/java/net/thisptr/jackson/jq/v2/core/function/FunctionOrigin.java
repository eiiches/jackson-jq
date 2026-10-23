package net.thisptr.jackson.jq.v2.core.function;

/**
 * Describes how a function entered the system — the mechanism by which it was registered,
 * independent of filtering concerns.
 *
 * <p>Together with the locator from {@link FunctionContext#getLocator()}, it uniquely identifies
 * the source of a function, allowing a {@link FunctionFilter} to discriminate between two functions
 * with the same {@link net.thisptr.jackson.jq.v2.spi.FunctionSignature} that come from different origins.</p>
 */
public enum FunctionOrigin {

	/**
	 * The function comes from a module resolved via a jq {@code import} or {@code include} statement.
	 * The locator is the module path string (e.g. {@code "jackson/uuid"}).
	 */
	PATH_IMPORT,

	/**
	 * The function comes from a module baked into the environment via
	 * {@link net.thisptr.jackson.jq.v2.core.EnvironmentBuilder#importModule(net.thisptr.jackson.jq.v2.spi.module.Module, String)}.
	 * The locator is the alias given at registration time (e.g. {@code "uuid_alias"}).
	 */
	ENV_IMPORT,

	/**
	 * The function comes from a module baked into the environment via
	 * {@link net.thisptr.jackson.jq.v2.core.EnvironmentBuilder#includeModule(net.thisptr.jackson.jq.v2.spi.module.Module)}.
	 * The locator is the fully-qualified class name of the module (e.g. {@code "com.example.UuidModule"}).
	 */
	ENV_INCLUDE,

	/**
	 * The function comes from a {@link FunctionLoader} added to the environment.
	 * The locator is the fully-qualified class name of the loader
	 * (e.g. {@code "net.thisptr.jackson.jq.v2.core.function.loaders.BuiltinFunctionLoader"}).
	 * Built-in jq functions fall into this category via {@code BuiltinFunctionLoader}.
	 */
	FUNCTION_LOADER,
}
