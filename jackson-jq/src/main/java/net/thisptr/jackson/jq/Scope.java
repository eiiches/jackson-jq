package net.thisptr.jackson.jq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import net.thisptr.jackson.jq.internal.annotations.Experimental;
import net.thisptr.jackson.jq.internal.misc.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.internal.module.loaders.NullModuleLoader;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.path.Path;

public class Scope {
	private static final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder()
			.addModule(JsonQueryJacksonModule.getInstance())
			.build();

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

	private static abstract class AbstractValueWithPath implements ValueWithPath {
		private final Path path;
		
		public AbstractValueWithPath (Path path) {
			this.path = path;
		}
		
		@Override
		public Path path() {
			return path;
		}
	}
	
	private static class ValueSupplierImpl extends AbstractValueWithPath {
		private final Supplier<JsonNode> valueSupplier;
		
		public ValueSupplierImpl(final Supplier<JsonNode> valueSupplier, final Path path) {
			super(path);
			this.valueSupplier = valueSupplier;
		}

		@Override
		public JsonNode value() {
			return valueSupplier.get();
		}
	}
	
	private static class ValueWithPathImpl extends AbstractValueWithPath {
		@JsonProperty("value")
		private final JsonNode value;
		
		public ValueWithPathImpl(final JsonNode value, final Path path) {
			super(path);
			this.value = value;
			
		}

		@Override
		public JsonNode value() {
			return value;
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

	@Experimental
	public Map<String, Function> getLocalFunctions() {
		if (functions == null)
			return new HashMap<>();
		return new HashMap<>(functions);
	}

	@Experimental
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
	
	public void setValue (final String name, Supplier<JsonNode> supplier) {
		setValueWithPath (name, supplier, null);
	}

	public void setValueWithPath(final String name, final JsonNode value, final Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueWithPathImpl(value, path));
	}
	
	public  void setValueWithPath(final String name, final Supplier<JsonNode> value, final Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueSupplierImpl(value, path));
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

	@Experimental
	public void setImportedData(final String name, final JsonNode data) {
		if (importedData == null)
			importedData = new HashMap<>();
		importedData.put(name, data);
	}

	@Experimental
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

	@Experimental
	public void addImportedModule(final String name, final Module module) {
		if (importedModules == null)
			importedModules = new HashMap<>();
		importedModules.computeIfAbsent(name, (dummy) -> new LinkedList<>()).addFirst(module);
	}

	@Experimental
	public List<Module> getImportedModules(final String name) { // the last import comes first
		final List<Module> modules = new ArrayList<>();
		getImportedModules(modules, name);
		return modules;
	}

	@Experimental
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

	@Experimental
	public void setModuleLoader(final ModuleLoader moduleLoader) {
		this.moduleLoader = moduleLoader;
	}

	@Experimental
	public ModuleLoader getModuleLoader() {
		if (this.moduleLoader != null)
			return this.moduleLoader;
		if (parentScope == null)
			return NullModuleLoader.getInstance();
		return parentScope.getModuleLoader();
	}

	@Experimental
	public Module getCurrentModule() {
		if (this.currentModule != null)
			return this.currentModule;
		if (parentScope == null)
			return null;
		return parentScope.getCurrentModule();
	}

	@Experimental
	public void setCurrentModule(final Module module) {
		this.currentModule = module;
	}
}
