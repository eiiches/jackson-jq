package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.internal.module.loaders.NullModuleLoader;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Scope<JsonNode> {
	private JsonProvider<JsonNode> jsonProvider;

	public JsonProvider<JsonNode> jsonProvider() {
		if (jsonProvider != null)
			return jsonProvider;
		if (parentScope == null)
			throw new IllegalStateException("JsonProvider is not set");
		return parentScope.jsonProvider();
	}

	public void setJsonProvider(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	private Map<String, String> debugFunctions() {
		Map<String, String> result = new TreeMap<>();
		for (Entry<String, Function> f : functions.entrySet())
			result.put(f.getKey(), f.getValue().toString());
		return result;
	}

	private Scope<JsonNode> parentScope;

	private Map<String, Function> functions;

	private Map<String, LinkedList<Module>> importedModules; // the last import comes first; the key is null when the module is loaded by an include statement.

	private Map<String, JsonNode> importedData; // the last import overwrites prior imports

	private ModuleLoader<JsonNode> moduleLoader;

	public interface ValueWithPath<JsonNode> {
		JsonNode value();

		Path path();
	}

	private abstract static class AbstractValueWithPath<JsonNode> implements ValueWithPath<JsonNode> {
		private Path path;

		AbstractValueWithPath (Path path) {
			this.path = path;
		}

		@Override
		public Path path() {
			return path;
		}
	}

	private static class ValueSupplierImpl<JsonNode> extends AbstractValueWithPath<JsonNode> {
		private Supplier<JsonNode> valueSupplier;

		ValueSupplierImpl(Supplier<JsonNode> valueSupplier, Path path) {
			super(path);
			this.valueSupplier = valueSupplier;
		}

		@Override
		public JsonNode value() {
			return valueSupplier.get();
		}
	}

	private static class ValueWithPathImpl<JsonNode> extends AbstractValueWithPath<JsonNode> {
		private JsonNode value;

		ValueWithPathImpl(JsonNode value, Path path) {
			super(path);
			this.value = value;

		}

		@Override
		public JsonNode value() {
			return value;
		}
	}

	private Map<String, ValueWithPath<JsonNode>> values;

	private Module currentModule;

	private Scope(Scope<JsonNode> parentScope) {
		this.parentScope = parentScope;
	}

	public static <JsonNode> Scope<JsonNode> newEmptyScope(JsonProvider<JsonNode> jsonProvider) {
		Scope<JsonNode> scope = new Scope<>(null);
		scope.setJsonProvider(jsonProvider);
		return scope;
	}

	public static <JsonNode> Scope<JsonNode> newChildScope(Scope<JsonNode> scope) {
		return new Scope<>(scope);
	}

	public void addFunction(String name, int n, Function q) {
		addFunction(name + "/" + n, q);
	}

	public void addFunction(String name, Function q) {
		if (functions == null)
			functions = new HashMap<>();
		functions.put(name, q);
	}

	public Function getFunction(String name, int nargs) {
		Function f = getFunctionRecursive(name + "/" + nargs);
		if (f != null)
			return f;
		return getFunctionRecursive(name);
	}

	public Map<String, Function> getLocalFunctions() {
		if (functions == null)
			return new HashMap<>();
		return new HashMap<>(functions);
	}

	public Scope<JsonNode> getParentScope() {
		return parentScope;
	}

	private Function getFunctionRecursive(String name) {
		if (functions != null) {
			Function q = functions.get(name);
			if (q != null)
				return q;
		}
		if (parentScope == null)
			return null;
		return parentScope.getFunctionRecursive(name);
	}

	public void setValue(String name, JsonNode value) {
		setValueWithPath(name, value, null);
	}

	public void setValue (String name, Supplier<JsonNode> supplier) {
		setValueWithPath (name, supplier, null);
	}

	public void setValueWithPath(String name, JsonNode value, Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueWithPathImpl<>(value, path));
	}

	public  void setValueWithPath(String name, Supplier<JsonNode> value, Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueSupplierImpl<>(value, path));
	}

	public ValueWithPath<JsonNode> getValueWithPath(String name) {
		if (values != null) {
			ValueWithPath<JsonNode> value = values.get(name);
			if (value != null)
				return value;
		}
		if (parentScope == null)
			return null;
		return parentScope.getValueWithPath(name);
	}

	public JsonNode getValue(String name) {
		ValueWithPath<JsonNode> value = getValueWithPath(name);
		if (value == null)
			return null;
		return value.value();
	}

	public void setImportedData(String name, JsonNode data) {
		if (importedData == null)
			importedData = new HashMap<>();
		importedData.put(name, data);
	}

	public JsonNode getImportedData(String name) {
		if (importedData != null) {
			JsonNode data = importedData.get(name);
			if (data != null)
				return data;
		}
		if (parentScope == null)
			return null;
		return parentScope.getImportedData(name);
	}

	public void addImportedModule(String name, Module module) {
		if (importedModules == null)
			importedModules = new HashMap<>();
		importedModules.computeIfAbsent(name, (dummy) -> new LinkedList<>()).addFirst(module);
	}

	public List<Module> getImportedModules(String name) { // the last import comes first
		List<Module> modules = new ArrayList<>();
		getImportedModules(modules, name);
		return modules;
	}

	private void getImportedModules(List<Module> modules, String name) {
		if (importedModules != null) {
			List<Module> localModules = importedModules.get(name);
			if (localModules != null) {
				modules.addAll(localModules);
			}
		}
		if (parentScope == null)
			return;
		parentScope.getImportedModules(modules, name);
	}

	public void setModuleLoader(ModuleLoader<JsonNode> moduleLoader) {
		this.moduleLoader = moduleLoader;
	}

	public ModuleLoader<JsonNode> getModuleLoader() {
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

	public void setCurrentModule(Module module) {
		this.currentModule = module;
	}
}
