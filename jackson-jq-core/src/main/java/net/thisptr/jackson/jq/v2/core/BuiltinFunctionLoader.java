package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.v2.core.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.v2.core.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.JqLibrary.JqFunc;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class BuiltinFunctionLoader {
	private static final BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	/**
	 * Load function definitions from the available providers
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	public Map<FunctionNameAndArity, FunctionFactory> listFunctionFactories(Version version) {
		Map<FunctionNameAndArity, FunctionFactory> result = new HashMap<>();

		for (FunctionFactory factory : ServiceLoader.load(FunctionFactory.class, BuiltinFunctionLoader.class.getClassLoader())) {
			FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
			for (FunctionRegistration reg : regs) {
				VersionRange versionRange = VersionRange.valueOf(reg.version());
				if (!versionRange.contains(version))
					continue;

				result.put(FunctionNameAndArity.of(reg.name(), reg.nargs()), factory);
			}
		}

		for (JqLibrary library : ServiceLoader.load(JqLibrary.class, BuiltinFunctionLoader.class.getClassLoader())) {
			for (JqFunc def : library.getFunctions()) {
				if (def.version != null && !def.version.contains(version))
					continue;
				result.put(FunctionNameAndArity.of(def.name, def.args.size()), createJqFunctionFactory(def, version));
			}
		}

		return result;
	}

	@Deprecated
	public void loadFunctions(Version version, Scope<?> scope) {
		listFunctionFactories(version).forEach((nameAndArity, factory) -> scope.addFunctionFactory(nameAndArity, factory));
	}

	private FunctionFactory createJqFunctionFactory(JqFunc def, Version version) {
		return new JsonQueryFunction<>(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body, version)), null);
	}
}
