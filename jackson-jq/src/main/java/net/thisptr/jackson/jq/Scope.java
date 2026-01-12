package net.thisptr.jackson.jq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.internal.annotations.Experimental;
import net.thisptr.jackson.jq.internal.module.loaders.NullModuleLoader;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.path.Path;

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
		final Map<String, String> result = new TreeMap<>();
		for (final Entry<String, Function<JsonNode>> f : functions.entrySet())
			result.put(f.getKey(), f.getValue().toString());
		return result;
	}

	private Scope<JsonNode> parentScope;

	private Map<String, Function<JsonNode>> functions;

	private Map<String, LinkedList<Module<JsonNode>>> importedModules; // the last import comes first; the key is null when the module is loaded by an include statement.

	private Map<String, JsonNode> importedData; // the last import overwrites prior imports

	private ModuleLoader<JsonNode> moduleLoader;

	public interface ValueWithPath<JsonNode> {
		JsonNode value();

		Path path();
	}

	private static abstract class AbstractValueWithPath<JsonNode> implements ValueWithPath<JsonNode> {
		private final Path path;

		public AbstractValueWithPath (Path path) {
			this.path = path;
		}

		@Override
		public Path path() {
			return path;
		}
	}

	private static class ValueSupplierImpl<JsonNode> extends AbstractValueWithPath<JsonNode> {
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

	private static class ValueWithPathImpl<JsonNode> extends AbstractValueWithPath<JsonNode> {
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

	private Map<String, ValueWithPath<JsonNode>> values;

	private Module<JsonNode> currentModule;

	private Scope(final Scope<JsonNode> parentScope) {
		this.parentScope = parentScope;
	}

	public static <JsonNode> Scope<JsonNode> newEmptyScope(JsonProvider<JsonNode> jsonProvider) {
		final Scope<JsonNode> scope = new Scope<>(null);
		scope.setJsonProvider(jsonProvider);
		return scope;
	}

	public static <JsonNode> Scope<JsonNode> newChildScope(final Scope<JsonNode> scope) {
		return new Scope<>(scope);
	}

	public void addFunction(final String name, final int n, final Function<JsonNode> q) {
		addFunction(name + "/" + n, q);
	}

	public void addFunction(final String name, final Function<JsonNode> q) {
		if (functions == null)
			functions = new HashMap<>();
		functions.put(name, q);
	}

	public Function<JsonNode> getFunction(final String name, final int nargs) {
		final Function<JsonNode> f = getFunctionRecursive(name + "/" + nargs);
		if (f != null)
			return f;
		return getFunctionRecursive(name);
	}

	@Experimental
	public Map<String, Function<JsonNode>> getLocalFunctions() {
		if (functions == null)
			return new HashMap<>();
		return new HashMap<>(functions);
	}

	@Experimental
	public Scope<JsonNode> getParentScope() {
		return parentScope;
	}

	private Function<JsonNode> getFunctionRecursive(final String name) {
		if (functions != null) {
			final Function<JsonNode> q = functions.get(name);
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
		values.put(name, new ValueWithPathImpl<>(value, path));
	}

	public  void setValueWithPath(final String name, final Supplier<JsonNode> value, final Path path) {
		if (values == null)
			values = new HashMap<>();
		values.put(name, new ValueSupplierImpl<>(value, path));
	}

	public ValueWithPath<JsonNode> getValueWithPath(final String name) {
		if (values != null) {
			final ValueWithPath<JsonNode> value = values.get(name);
			if (value != null)
				return value;
		}
		if (parentScope == null)
			return null;
		return parentScope.getValueWithPath(name);
	}

	public JsonNode getValue(final String name) {
		final ValueWithPath<JsonNode> value = getValueWithPath(name);
		if (value == null)
			return null;
		return value.value();
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
	public void addImportedModule(final String name, final Module<JsonNode> module) {
		if (importedModules == null)
			importedModules = new HashMap<>();
		importedModules.computeIfAbsent(name, (dummy) -> new LinkedList<>()).addFirst(module);
	}

	@Experimental
	public List<Module<JsonNode>> getImportedModules(final String name) { // the last import comes first
		final List<Module<JsonNode>> modules = new ArrayList<>();
		getImportedModules(modules, name);
		return modules;
	}

	@Experimental
	private void getImportedModules(final List<Module<JsonNode>> modules, final String name) {
		if (importedModules != null) {
			final List<Module<JsonNode>> localModules = importedModules.get(name);
			if (localModules != null) {
				modules.addAll(localModules);
			}
		}
		if (parentScope == null)
			return;
		parentScope.getImportedModules(modules, name);
	}

	@Experimental
	public void setModuleLoader(final ModuleLoader<JsonNode> moduleLoader) {
		this.moduleLoader = moduleLoader;
	}

	@Experimental
	public ModuleLoader<JsonNode> getModuleLoader() {
		if (this.moduleLoader != null)
			return this.moduleLoader;
		if (parentScope == null)
			return (ModuleLoader<JsonNode>) NullModuleLoader.getInstance();
		return parentScope.getModuleLoader();
	}

	@Experimental
	public Module<JsonNode> getCurrentModule() {
		if (this.currentModule != null)
			return this.currentModule;
		if (parentScope == null)
			return null;
		return parentScope.getCurrentModule();
	}

	@Experimental
	public void setCurrentModule(final Module<JsonNode> module) {
		this.currentModule = module;
	}
}
