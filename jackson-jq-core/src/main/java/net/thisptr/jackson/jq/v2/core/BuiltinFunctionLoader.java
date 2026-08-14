package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.v2.core.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.JqLibrary.JqFunc;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class BuiltinFunctionLoader {
	private static BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	private BuiltinFunctionLoader() {
	}

	/**
	 * Load function definitions from the available providers
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	public Map<String, Function> listFunctions(ClassLoader classLoader, Version version, Scope closureScope) {
		Map<String, Function> functions = new HashMap<>();
		functions.putAll(loadFunctionsFromJqLibrary(classLoader, version, closureScope));
		functions.putAll(loadFunctionsFromServiceLoader(classLoader, version));
		return functions;
	}

	public Map<String, Function> listFunctions(Version version, Scope closureScope) {
		return listFunctions(BuiltinFunctionLoader.class.getClassLoader(), version, closureScope);
	}

	public void loadFunctions(Version version, Scope closureScope) {
		listFunctions(version, closureScope).forEach(closureScope::addFunction);
	}

	public void loadFunctions(ClassLoader classLoader, Version version, Scope closureScope) {
		listFunctions(classLoader, version, closureScope).forEach(closureScope::addFunction);
	}

	private static String @Nullable [] extractFunctionNamesFromAnnotationIfVersionMatch(Function fn, Version version) {
		FunctionRegistration annotation = fn.getClass().getAnnotation(FunctionRegistration.class);
		if (annotation == null)
			return null;
		if (!annotation.version().isEmpty()) {
			VersionRange range = VersionRange.valueOf(annotation.version());
			if (!range.contains(version))
				return new String[0];
		}
		return annotation.value();
	}

	private Map<String, Function> loadFunctionsFromServiceLoader(ClassLoader classLoader, Version version) {
		Map<String, Function> functions = new HashMap<>();
		for (Function fn : ServiceLoader.load(Function.class, classLoader)) {
			String[] names = extractFunctionNamesFromAnnotationIfVersionMatch(fn, version);
			if (names == null) // i.e. no annotations found
				continue;

			for (String name : names)
				functions.put(name, fn);
		}
		return functions;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Function> loadFunctionsFromJqLibrary(ClassLoader classLoader, Version version, Scope closureScope) {
		try {
			Map<String, Function> functions = new HashMap<>();
			for (JqLibrary library : ServiceLoader.load(JqLibrary.class, classLoader)) {
				for (JqFunc def : library.getFunctions()) {
					if (def.version != null && !VersionRange.valueOf(def.version).contains(version))
						continue;
					functions.put(def.name + "/" + def.args.size(), new JsonQueryFunction(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body, version)), closureScope));
				}
			}
			return functions;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load macros", e);
		}
	}
}
