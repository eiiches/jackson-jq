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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.internal.misc.VersionRangeDeserializer;

public class BuiltinFunctionLoader {
	private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class JqJson {
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JqFuncDef {
			@JsonProperty("name")
			public String name;

			@JsonProperty("args")
			public List<String> args = new ArrayList<>();

			@JsonProperty("body")
			public String body;

			@JsonProperty("version")
			@JsonDeserialize(using = VersionRangeDeserializer.class)
			public VersionRange version;
		}

		@JsonProperty("functions")
		public List<JqFuncDef> functions = new ArrayList<>();
	}

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
	public Map<String, Function> loadFunctions(final ClassLoader classLoader, final Version version, final Scope closureScope) {
		final Map<String, Function> functions = new HashMap<>();
		loadMacros(functions, classLoader, version, closureScope);
		loadBuiltinFunctions(functions, version, classLoader);
		return functions;
	}

	private static List<JqJson> loadConfig(final ClassLoader loader, final String path) throws IOException {
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
			final MappingIterator<JqJson> iter2 = DEFAULT_MAPPER.readValues(DEFAULT_MAPPER.getFactory().createParser(buffer.toString()), JqJson.class);
			while (iter2.hasNext()) {
				result.add(iter2.next());
			}
		}
		return result;
	}

	private void loadBuiltinFunctions(final Map<String, Function> functions, final Version version, final ClassLoader classLoader) {
		for (final Function fn : ServiceLoader.load(Function.class, classLoader)) {
			final BuiltinFunction annotation = fn.getClass().getAnnotation(BuiltinFunction.class);
			if (annotation == null)
				continue;
			if (!annotation.version().isEmpty()) {
				final VersionRange range = VersionRange.valueOf(annotation.version());
				if (!range.contains(version))
					continue;
			}
			for (final String name : annotation.value())
				functions.put(name, fn);
		}
	}

	private void loadMacros(final Map<String, Function> functions, final ClassLoader classLoader, final Version version, final Scope closureScope) {
		try {
			final List<JqJson> configs = loadConfig(classLoader, CONFIG_PATH);
			for (final JqJson jqJson : configs) {
				for (final JqJson.JqFuncDef def : jqJson.functions) {
					if (def.version != null && !def.version.contains(version))
						continue;
					functions.put(def.name + "/" + def.args.size(), new JsonQueryFunction(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body, version)), closureScope));
				}
			}
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load macros", e);
		}
	}
}
