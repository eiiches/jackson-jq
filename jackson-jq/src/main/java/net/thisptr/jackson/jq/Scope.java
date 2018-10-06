package net.thisptr.jackson.jq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.path.Path;

public class Scope {
	private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
			.registerModule(JsonQueryJacksonModule.getInstance());

	@BuiltinFunction("debug_scope/0")
	public static class DebugScopeFunction implements Function {
		@Override
		public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
			final Map<String, Object> info = new HashMap<>();
			info.put("scope", scope);
			info.put("input", in);
			output.emit(DEFAULT_MAPPER.valueToTree(info), null);
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
	private Map<String, Function> functions;

	public interface ValueWithPath {
		JsonNode value();

		Path path();
	}

	private static class ValueWithPathImpl implements ValueWithPath {
		@JsonProperty("value")
		private final JsonNode value;

		@JsonProperty("path")
		private final Path path;

		public ValueWithPathImpl(final JsonNode value, final Path path) {
			this.value = value;
			this.path = path;
		}

		@Override
		public JsonNode value() {
			return value;
		}

		@Override
		public Path path() {
			return path;
		}
	}

	@JsonProperty("variables")
	private Map<String, ValueWithPath> values;

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
		addFunction(name + "/" + n, q);
	}

	public void addFunction(final String name, final Function q) {
		if (functions == null)
			functions = new HashMap<>();
		functions.put(name, q);
	}

	public Function getFunction(final String name, final int nargs) {
		final Function f = getFunctionRecursive(name + "/" + nargs);
		if (f != null)
			return f;
		return getFunctionRecursive(name);
	}

	private Function getFunctionRecursive(final String name) {
		if (functions != null) {
			final Function q = functions.get(name);
			if (q != null)
				return q;
		}
		if (parentScope == null)
			return null;
		return parentScope.getFunctionRecursive(name);
	}

	public void setValue(final String name, final JsonNode value) {
		setValueWithPath(name, value, null);
	}

	public void setValueWithPath(final String name, final JsonNode value, final Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueWithPathImpl(value, path));
	}

	public ValueWithPath getValueWithPath(final String name) {
		if (values != null) {
			final ValueWithPath value = values.get(name);
			if (value != null)
				return value;
		}
		if (parentScope == null)
			return null;
		return parentScope.getValueWithPath(name);
	}

	public JsonNode getValue(final String name) {
		final ValueWithPath value = getValueWithPath(name);
		if (value == null)
			return null;
		return value.value();
	}

	@Deprecated
	public void loadFunctions(final ClassLoader classLoader, final Version version) {
		new BuiltinFunctionLoader().loadFunctions(classLoader, version, this).forEach(this::addFunction);
	}

	@JsonIgnore
	public ObjectMapper getObjectMapper() {
		return mapper;
	}
}
