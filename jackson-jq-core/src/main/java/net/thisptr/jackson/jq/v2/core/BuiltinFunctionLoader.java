package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.v2.core.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.v2.core.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.internal.InternalJqLibrary;
import net.thisptr.jackson.jq.v2.spi.internal.InternalJqLibrary.JqFunc;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class BuiltinFunctionLoader {
	private static BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	private BuiltinFunctionLoader() {}

	/**
	 * Load function definitions from the available providers
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	public Map<String, Function> listFunctions(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		final Map<String, Function> functions = new HashMap<>();
		functions.putAll(loadFunctionsFromJqLibrary(classLoader, version, closureScope));
		functions.putAll(loadFunctionsFromServiceLoader(classLoader, version));
		return functions;
	}

	public Map<String, Function> listFunctions(final Version version, final Scope closureScope) {
		return listFunctions(BuiltinFunctionLoader.class.getClassLoader(), version, closureScope);
	}

	public void loadFunctions(final Version version, final Scope closureScope) {
		listFunctions(version, closureScope).forEach(closureScope::addFunction);
	}

	public void loadFunctions(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		listFunctions(classLoader, version, closureScope).forEach(closureScope::addFunction);
	}

	private static String[] extractFunctionNamesFromAnnotationIfVersionMatch(Function fn, final Version version) {
		final FunctionRegistration annotation = fn.getClass().getAnnotation(FunctionRegistration.class);
		if (annotation == null)
			return null;
		if (!annotation.version().isEmpty()) {
			final VersionRange range = VersionRange.valueOf(annotation.version());
			if (!range.contains(version))
				return new String[0];
		}
		return annotation.value();
	}

	@SuppressWarnings("deprecation")
	private static String[] extractFunctionNamesFromDeprecatedAnnotationIfVersionMatch(Function fn, final Version version) {
		final net.thisptr.jackson.jq.v2.core.internal.BuiltinFunction annotation = fn.getClass().getAnnotation(net.thisptr.jackson.jq.v2.core.internal.BuiltinFunction.class);
		if (annotation == null)
			return null;
		if (!annotation.version().isEmpty()) {
			final VersionRange range = VersionRange.valueOf(annotation.version());
			if (!range.contains(version))
				return new String[0];
		}
		return annotation.value();
	}

	/**
	 * Do not use this method. This method is only for Quarkus extension.
	 */
	public Map<String, Function> loadFunctionsFromServiceLoader(final ClassLoader classLoader, final Version version) {
		final Map<String, Function> functions = new HashMap<>();
		for (final Function fn : ServiceLoader.load(Function.class, classLoader)) {
			String[] names = extractFunctionNamesFromAnnotationIfVersionMatch(fn, version);
			if (names == null) { // i.e. if annotation is missing,
				// Look for deprecated annotation as well for compatibility reasons. TODO: Delete this in 1.0.0 release.
				names = extractFunctionNamesFromDeprecatedAnnotationIfVersionMatch(fn, version);
			}

			if (names == null) // i.e. no annotations found
				continue;

			for (final String name : names)
				functions.put(name, fn);
		}
		return functions;
	}

	/**
	 * Do not use this method. This method is only for Quarkus extension.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Function> loadFunctionsFromJqLibrary(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		try {
			final Map<String, Function> functions = new HashMap<>();
			for (final InternalJqLibrary library : ServiceLoader.load(InternalJqLibrary.class, classLoader)) {
				for (final JqFunc def : library.getFunctions()) {
					if (def.version != null && !VersionRange.valueOf(def.version).contains(version))
						continue;
					functions.put(def.name + "/" + def.args.size(), new JsonQueryFunction(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body, version)), closureScope));
				}
			}
			return functions;
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load macros", e);
		}
	}

	/**
	 * Do not use this method. This method is only for Quarkus extension.
	 *
	 * @deprecated use {@link #loadFunctionsFromJqLibrary(ClassLoader, Version, Scope)}
	 */
	@Deprecated
	public Map<String, Function> loadFunctionsFromJsonJq(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		return loadFunctionsFromJqLibrary(classLoader, version, closureScope);
	}
}
