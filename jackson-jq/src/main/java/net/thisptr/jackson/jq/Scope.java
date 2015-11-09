package net.thisptr.jackson.jq;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonQueryFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class Scope {
	private static final ObjectMapper defaultMapper = new ObjectMapper();

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
			return Collections.singletonList(defaultMapper.valueToTree(info));
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
	private ObjectMapper mapper = defaultMapper;

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
	}

	public static Scope rootScope() {
		return RootScopeHolder.scope;
	}

	private static void classes(final List<Class<?>> result, final Class<?> root) {
		result.add(root);
		for (final Class<?> clazz : root.getDeclaredClasses())
			classes(result, clazz);
	}

	private static List<Class<?>> classes() throws IOException {
		final List<Class<?>> result = new ArrayList<>();
		for (final ClassInfo classInfo : ClassPath.from(Scope.class.getClassLoader()).getTopLevelClassesRecursive(Scope.class.getPackage().getName())) {
			final Class<?> clazz = classInfo.load();
			classes(result, clazz);
		}
		return result;
	}

	private void loadDefault() {
		try {
			for (final Class<?> clazz : classes()) {
				if (!Function.class.isAssignableFrom(clazz))
					continue;
				final BuiltinFunction annotation = clazz.getAnnotation(BuiltinFunction.class);
				if (annotation == null)
					continue;
				for (final String name : annotation.value())
					addFunction(name, (Function) clazz.newInstance());
			}
			try (final InputStream is = Scope.class.getClassLoader().getResourceAsStream("jq.json")) {
				final JqJson jqJson = defaultMapper.readValue(is, JqJson.class);
				for (final JqJson.JqFuncDef def : jqJson.functions)
					addFunction(def.name, def.args.size(), new JsonQueryFunction(def.name, def.args, JsonQuery.compile(def.body)));
			}
		} catch (final IOException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to instanciate default Scope object", e);
		}
	}
}
