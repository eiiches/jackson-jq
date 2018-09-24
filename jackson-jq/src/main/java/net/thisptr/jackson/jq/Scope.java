package net.thisptr.jackson.jq;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class Scope {
	private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

	@BuiltinFunction("debug_scope/0")
	public static class DebugScopeFunction implements Function {
		@Override
		public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
			final Map<String, Object> info = new HashMap<>();
			info.put("scope", scope);
			info.put("input", in);
			output.emit(DEFAULT_MAPPER.valueToTree(info));
		}
	}

	@JsonProperty("functions")
	@SuppressWarnings("unused")
	private Map<String, String> debugFunctions() {
		final Map<String, String> result = new TreeMap<>();
		for (final Entry<String, Function> f : functions.entrySet())
			result.put(f.getKey(), f.getValue().toString());
		return result;
	}

	@JsonProperty("parent")
	private Scope parentScope;

	@JsonIgnore
	private Map<String, Function> functions = new HashMap<>();

	@JsonProperty("variables")
	private Map<String, JsonNode> values = new HashMap<>();

	@JsonIgnore
	private ObjectMapper mapper = DEFAULT_MAPPER;

	private Scope(final Scope parentScope) {
		this.parentScope = parentScope;
	}

	public static Scope newEmptyScope() {
		return new Scope(null);
	}

	public static Scope newChildScope(final Scope scope) {
		return new Scope(scope);
	}

	public void addFunction(final String name, final int n, final Function q) {
		functions.put(name + "/" + n, q);
	}

	public void addFunction(final String name, final Function q) {
		functions.put(name, q);
	}

	public Function getFunction(final String name, final int nargs) {
		final Function f = getFunctionRecursive(name, nargs);
		if (f != null)
			return f;
		return getFunctionRecursive(name);
	}

	private Function getFunctionRecursive(final String name, final int nargs) {
		final Function q = functions.get(name + "/" + nargs);
		if (q == null && parentScope != null)
			return parentScope.getFunctionRecursive(name, nargs);
		return q;
	}

	private Function getFunctionRecursive(final String name) {
		final Function q = functions.get(name);
		if (q == null && parentScope != null)
			return parentScope.getFunctionRecursive(name);
		return q;
	}

	public void setValue(final String name, final JsonNode value) {
		values.put(name, value);
	}

	public JsonNode getValue(final String name) {
		final JsonNode value = values.get(name);
		if (value == null && parentScope != null)
			return parentScope.getValue(name);
		return value;
	}

	@JsonIgnore
	public ObjectMapper getObjectMapper() {
		return mapper;
	}

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
	public void loadFunctions(final ClassLoader classLoader) {
		loadMacros(classLoader);
		loadBuiltinFunctions(classLoader);
	}

	private static List<JqJson> loadConfig(final ClassLoader loader) throws IOException {
		final List<JqJson> result = new ArrayList<>();
		final Enumeration<URL> iter = loader.getResources(CONFIG_PATH);
		while (iter.hasMoreElements()) {
			try (final InputStream is = iter.nextElement().openStream()) {
				final MappingIterator<JqJson> iter2 = DEFAULT_MAPPER.readValues(DEFAULT_MAPPER.getFactory().createParser(is), JqJson.class);
				while (iter2.hasNext()) {
					result.add(iter2.next());
				}
			}
		}
		return result;
	}

	private void loadBuiltinFunctions(final ClassLoader classLoader) {
		for (final Function fn : ServiceLoader.load(Function.class, classLoader)) {
			final BuiltinFunction annotation = fn.getClass().getAnnotation(BuiltinFunction.class);
			if (annotation == null)
				continue;
			for (final String name : annotation.value())
				addFunction(name, fn);
		}
	}

	private void loadMacros(final ClassLoader classLoader) {
		try {
			final List<JqJson> configs = loadConfig(classLoader);
			for (final JqJson jqJson : configs) {
				for (final JqJson.JqFuncDef def : jqJson.functions)
					addFunction(def.name, def.args.size(), new JsonQueryFunction(def.name, def.args, new IsolatedScopeQuery(ExpressionParser.compile(def.body)), this));
			}
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load macros", e);
		}
	}
}
