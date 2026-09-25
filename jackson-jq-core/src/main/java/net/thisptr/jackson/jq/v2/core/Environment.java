package net.thisptr.jackson.jq.v2.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Read-only view of a compilation environment. Build one with {@link EnvironmentBuilder}.
 */
public interface Environment<JsonNode> {
	JsonProvider<JsonNode> getJsonProvider();

	Version getJqVersion();

	/**
	 * The module loaders this environment consults, in the order they are asked.
	 */
	List<ModuleLoader<JsonNode>> getModuleLoaders();

	/**
	 * The function loaders this environment consults, in the order they are asked.
	 * <p>
	 * These are not the instances handed to {@code EnvironmentBuilder.addFunctionLoader}: each is
	 * wrapped in a memoizing decorator private to this environment, because a loader is asked for a
	 * whole registry on every unresolved call and discovering one can be expensive.
	 */
	List<FunctionLoader> getFunctionLoaders();

	/**
	 * The variables this environment declares without a value, each with the type it was declared at.
	 */
	Map<String, Type> getDeclaredVariables();

	Set<FunctionSignature> getDeclaredFunctions();

	Map<String, Variable<JsonNode>> getVariables();

	Map<FunctionSignature, Function> getFunctions();

	Map<FunctionSignature, JqFunction> getJqFunctions();

	Map<String, Constant<JsonNode>> getConstants();

	interface Constant<JsonNode> {
		/**
		 * The type this constant was registered at, which is read off {@link #getConstantValue} unless
		 * the registration named one. It is what compile-time type checking is told; nothing validates
		 * the value against it.
		 */
		Type getType();

		JsonNode getConstantValue();
	}

	interface Variable<JsonNode> {
		/**
		 * The type this variable was registered at, {@link Type#ANY} unless the registration named one.
		 * It is what compile-time type checking is told; nothing validates what the supplier returns
		 * against it.
		 */
		Type getType();

		Supplier<JsonNode> getValue();
	}

	Map<String, Module> getImportedModules();

	/**
	 * Compiles {@code expression} with default options.
	 *
	 * @param expression the jq expression to compile
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled; other runtime
	 * exceptions and stack overflows during compilation are wrapped in a {@code JsonQueryException}
	 */
	default JsonQuery<JsonNode> compile(String expression) throws JsonQueryException {
		return compile(expression, CompileOptions.getDefaultInstance());
	}

	/**
	 * Compiles {@code expression}.
	 *
	 * @param expression the jq expression to compile
	 * @param options settings for this compilation, including who receives its diagnostics
	 * @return the compiled query
	 * @throws JsonQueryException if {@code expression} cannot be parsed or compiled; other runtime
	 * exceptions and stack overflows during compilation are wrapped in a {@code JsonQueryException}
	 */
	JsonQuery<JsonNode> compile(String expression, CompileOptions options) throws JsonQueryException;
}
