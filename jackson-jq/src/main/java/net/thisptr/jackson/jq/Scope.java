package net.thisptr.jackson.jq;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;

public class Scope {
	private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

	private static final class RootScopeHolder {
		public static final Scope scope = new Scope(null);
		static {
			try {
				scope.loadDefault();
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	@BuiltinFunction("debug_scope/0")
	public static class DebugScopeFunction implements Function {
		@Override
		public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
			final Map<String, Object> info = new HashMap<>();
			info.put("scope", scope);
			info.put("input", in);
			return Collections.singletonList(DEFAULT_MAPPER.valueToTree(info));
		}
	}

	@JsonProperty("functions")
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

	public Scope() {
		this(RootScopeHolder.scope);
	}

	public Scope(final Scope parentScope) {
		this.parentScope = parentScope;
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

		@JsonProperty("classes")
		public List<String> classes = new ArrayList<>();
	}

	public static Scope rootScope() {
		return RootScopeHolder.scope;
	}

	private static Collection<Class<?>> classes(final List<JqJson> configs) throws IOException {
		final Set<String> clazzes = new HashSet<>();
		for (final JqJson jqJson : configs) {
			if (jqJson.classes == null)
				continue;
			for (final String className : jqJson.classes) {
				clazzes.add(className);
			}
		}
		final List<Class<?>> result = new ArrayList<>();
		for (final String className : clazzes) {
			try {
				result.add(Class.forName(className));
			} catch (Throwable th) {
				System.err.println("Failed to load class " + className + ": " + th.getClass().getSimpleName() + ": " + th.getMessage());
				continue;
			}
		}
		return result;
	}

	private static List<JqJson> readConfig() throws IOException {
		final List<JqJson> result = new ArrayList<>();
		final ClassLoader loader = MoreObjects.firstNonNull(
	        	Thread.currentThread().getContextClassLoader(),
	        	Scope.class.getClassLoader());
		final Enumeration<URL> iter = loader.getResources("jq.json");
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

	private void loadDefault() {
		try {
			final List<JqJson> configs = readConfig();
			for (final Class<?> clazz : classes(configs)) {
				if (!Function.class.isAssignableFrom(clazz))
					continue;
				final BuiltinFunction annotation = clazz.getAnnotation(BuiltinFunction.class);
				if (annotation == null)
					continue;
				for (final String name : annotation.value())
					addFunction(name, (Function) clazz.newInstance());
			}
			for (final JqJson jqJson : configs) {
				for (final JqJson.JqFuncDef def : jqJson.functions)
					addFunction(def.name, def.args.size(), new JsonQueryFunction(def.name, def.args, JsonQuery.compile(def.body)));
			}
		} catch (final IOException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to instanciate default Scope object", e);
		}
	}
}
