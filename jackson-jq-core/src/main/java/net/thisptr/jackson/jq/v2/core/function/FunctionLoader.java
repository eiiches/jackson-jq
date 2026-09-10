package net.thisptr.jackson.jq.v2.core.function;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Supplies version-specific Java and jq function definitions to the compiler.
 *
 * <p>The two registries are intentionally separate. {@link #getFunctions(Version)} returns only
 * Java-implemented functions, while {@link #getJqFunctions(Version)} returns only raw jq definitions.
 * The compiler is responsible for compiling and specializing the latter.</p>
 */
public interface FunctionLoader {
	Map<FunctionSignature, Function> getFunctions(Version jqVersion);

	Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion);
}
