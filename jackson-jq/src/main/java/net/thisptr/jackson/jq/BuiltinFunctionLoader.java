package net.thisptr.jackson.jq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.JqJson;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

/**
 * Use {@code BuiltinFunctionLoader.getInstance()} to obtain the instance.
 */
public class BuiltinFunctionLoader {
	private static BuiltinFunctionLoader INSTANCE = new BuiltinFunctionLoader();

	public static BuiltinFunctionLoader getInstance() {
		return INSTANCE;
	}

	private BuiltinFunctionLoader() {}

	private static final String CONFIG_PATH = resolvePath(Scope.class, "jq.json");

	/**
	 * Dynamically resolve the path for a resource as packages may be relocated, e.g. by
	 * the maven-shade-plugin.
	 */
	private static String resolvePath(final Class<?> clazz, final String name) {
		final String base = clazz.getName();
		return base.substring(0, base.lastIndexOf('.')).replace('.', '/') + '/' + name;
	}

	/**
	 * Load function definitions from the default resource
	 * from an arbitrary {@link ClassLoader}.
	 * E.g. in an OSGi context this may be the Bundle's {@link ClassLoader}.
	 */
	public Map<String, Function> listFunctions(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		final Map<String, Function> functions = new HashMap<>();
		functions.putAll(loadFunctionsFromJsonJq(classLoader, version, closureScope));
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

	private static <JsonNode> List<JqJson> loadConfig(final JsonProvider<JsonNode> jsonProvider, final ClassLoader loader, final String path) throws Exception {
		final List<JqJson> result = new ArrayList<>();
		final Enumeration<URL> iter = loader.getResources(path);
		while (iter.hasMoreElements()) {
			final StringBuilder buffer = new StringBuilder();
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(iter.nextElement().openStream(), StandardCharsets.UTF_8))) {
				while (true) {
					final String line = reader.readLine();
					if (line == null)
						break;
					if (line.startsWith("#"))
						continue;
					buffer.append(line);
					buffer.append('\n');
				}
			}
			final List<JsonNode> jsonNodes = jsonProvider.readMultipleValues(buffer.toString());
			for (final JsonNode node : jsonNodes) {
				result.add(parseJqJson(jsonProvider, node));
			}
		}
		return result;
	}

	private static <JsonNode> JqJson parseJqJson(final JsonProvider<JsonNode> jsonProvider, final JsonNode node) {
		final JqJson jqJson = new JqJson();
		final JsonNode functionsNode = jsonProvider.get(node, "functions");
		if (functionsNode != null && jsonProvider.getNodeType(functionsNode) == JsonNodeType.ARRAY) {
			for (final JsonNode funcNode : jsonProvider.iterate(functionsNode)) {
				final JqJson.JqFuncDef def = new JqJson.JqFuncDef();
				final JsonNode nameNode = jsonProvider.get(funcNode, "name");
				if (nameNode != null) {
					def.name = jsonProvider.asText(nameNode);
				}
				final JsonNode bodyNode = jsonProvider.get(funcNode, "body");
				if (bodyNode != null) {
					def.body = jsonProvider.asText(bodyNode);
				}
				final JsonNode argsNode = jsonProvider.get(funcNode, "args");
				if (argsNode != null && jsonProvider.getNodeType(argsNode) == JsonNodeType.ARRAY) {
					for (final JsonNode argNode : jsonProvider.iterate(argsNode)) {
						def.args.add(jsonProvider.asText(argNode));
					}
				}
				final JsonNode versionNode = jsonProvider.get(funcNode, "version");
				if (versionNode != null && jsonProvider.getNodeType(versionNode) == JsonNodeType.STRING) {
					def.version = VersionRange.valueOf(jsonProvider.asText(versionNode));
				}
				jqJson.functions.add(def);
			}
		}
		return jqJson;
	}

	private static String[] extractFunctionNamesFromAnnotationIfVersionMatch(Function fn, final Version version) {
		final net.thisptr.jackson.jq.BuiltinFunction annotation = fn.getClass().getAnnotation(net.thisptr.jackson.jq.BuiltinFunction.class);
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
		final net.thisptr.jackson.jq.internal.BuiltinFunction annotation = fn.getClass().getAnnotation(net.thisptr.jackson.jq.internal.BuiltinFunction.class);
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
	public Map<String, Function> loadFunctionsFromJsonJq(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		try {
			final Map<String, Function> functions = new HashMap<>();
			final List<JqJson> configs = loadConfig(closureScope.jsonProvider(), classLoader, CONFIG_PATH);
			for (final JqJson jqJson : configs) {
				for (final JqJson.JqFuncDef def : jqJson.functions) {
					if (def.version != null && !def.version.contains(version))
						continue;
					functions.put(def.name + "/" + def.args.size(), new JsonQueryFunction(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body, version)), closureScope));
				}
			}
			return functions;
		} catch (final Exception e) {
			throw new RuntimeException("Failed to load macros", e);
		}
	}
}
