package net.thisptr.jackson.jq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.internal.misc.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.internal.module.loaders.NullModuleLoader;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.path.Path;

public class Scope {
	private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
			.registerModule(JsonQueryJacksonModule.getInstance());

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
	private Map<String, Function> functions;

	@JsonIgnore
	private Map<String, LinkedList<Module>> importedModules; // the last import comes first; the key is null when the module is loaded by an include statement.

	@JsonIgnore
	private Map<String, JsonNode> importedData; // the last import overwrites prior imports

	@JsonIgnore
	private ModuleLoader moduleLoader;

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

	private Module currentModule;

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

	public Map<String, Function> getLocalFunctions() {
		if (functions == null)
			return new HashMap<>();
		return new HashMap<>(functions);
	}

	public Scope getParentScope() {
		return parentScope;
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

	@JsonIgnore
	public ObjectMapper getObjectMapper() {
		return mapper;
	}

	public void setImportedData(final String name, final JsonNode data) {
		if (importedData == null)
			importedData = new HashMap<>();
		importedData.put(name, data);
	}

	public JsonNode getImportedData(final String name) {
		if (importedData != null) {
			final JsonNode data = importedData.get(name);
			if (data != null)
				return data;
		}
		if (parentScope == null)
			return null;
		return parentScope.getImportedData(name);
	}

	public void addImportedModule(final String name, final Module module) {
		if (importedModules == null)
			importedModules = new HashMap<>();
		importedModules.computeIfAbsent(name, (dummy) -> new LinkedList<>()).addFirst(module);
	}

	public List<Module> getImportedModules(final String name) { // the last import comes first
		final List<Module> modules = new ArrayList<>();
		getImportedModules(modules, name);
		return modules;
	}

	private void getImportedModules(final List<Module> modules, final String name) {
		if (importedModules != null) {
			final List<Module> localModules = importedModules.get(name);
			if (localModules != null) {
				modules.addAll(localModules);
			}
		}
		if (parentScope == null)
			return;
		parentScope.getImportedModules(modules, name);
	}

	public void setModuleLoader(final ModuleLoader moduleLoader) {
		this.moduleLoader = moduleLoader;
	}

	public ModuleLoader getModuleLoader() {
		if (this.moduleLoader != null)
			return this.moduleLoader;
		if (parentScope == null)
			return NullModuleLoader.getInstance();
		return parentScope.getModuleLoader();
	}

	public Module getCurrentModule() {
		if (this.currentModule != null)
			return this.currentModule;
		if (parentScope == null)
			return null;
		return parentScope.getCurrentModule();
	}

	public void setCurrentModule(final Module module) {
		this.currentModule = module;
	}
}
