package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.internal.module.loaders.NullModuleLoader;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Scope<JsonNode> {
	private @Nullable JsonProvider<JsonNode> jsonProvider;

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

	private @Nullable Scope<JsonNode> parentScope;

	private @Nullable Map<String, FunctionFactory> functions;

	private @Nullable Map<@Nullable String, LinkedList<Module>> importedModules; // the last import comes first; the key is null when the module is loaded by an include statement.

	private @Nullable Map<String, JsonNode> importedData; // the last import overwrites prior imports

	private @Nullable ModuleLoader<JsonNode> moduleLoader;

	public interface ValueWithPath<JsonNode> {
		JsonNode value();

		@Nullable Path<JsonNode> path();
	}

	private @Nullable EvaluationFrame<JsonNode> evaluationFrame;

	public @Nullable EvaluationFrame<JsonNode> getEvaluationFrame() {
		if (evaluationFrame != null)
			return evaluationFrame;
		if (parentScope != null)
			return parentScope.getEvaluationFrame();
		return null;
	}

	private @Nullable Module currentModule;

	private Scope(@Nullable Scope<JsonNode> parentScope) {
		this.parentScope = parentScope;
	}

	public static <JsonNode> Scope<JsonNode> newEmptyScope(@Nullable JsonProvider<JsonNode> jsonProvider) {
		Scope<JsonNode> scope = new Scope<>(null);
		if (jsonProvider != null)
			scope.setJsonProvider(jsonProvider);
		return scope;
	}

	public static <JsonNode> Scope<JsonNode> newChildScope(Scope<JsonNode> scope) {
		return new Scope<>(scope);
	}

	public void addFunctionFactory(FunctionNameAndArity nameAndArity, FunctionFactory q) {
		addFunctionFactory(nameAndArity.toString(), q);
	}

	public void addFunctionFactory(String name, int n, FunctionFactory q) {
		addFunctionFactory(name + "/" + n, q);
	}

	public void addFunctionFactory(String name, FunctionFactory q) {
		if (functions == null)
			functions = new HashMap<>();
		functions.put(name, q);
	}

	public @Nullable FunctionFactory getFunctionFactory(String name, int nargs) {
		FunctionFactory f = getFunctionFactoryRecursive(name + "/" + nargs);
		if (f != null)
			return f;
		return getFunctionFactoryRecursive(name);
	}

	public Map<String, FunctionFactory> getLocalFunctionFactories() {
		if (functions == null)
			return new HashMap<>();
		return new HashMap<>(functions);
	}

	private @Nullable FunctionFactory getFunctionFactoryRecursive(String name) {
		if (functions != null) {
			FunctionFactory q = functions.get(name);
			if (q != null)
				return q;
		}
		if (parentScope == null)
			return null;
		return parentScope.getFunctionFactoryRecursive(name);
	}

	public void setValue(int slot, JsonNode value) {
		setValueWithPath(slot, value, null, 0);
	}

	public void setValue(int slot, JsonNode value, int frameSize) {
		setValueWithPath(slot, value, null, frameSize);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path) {
		setValueWithPath(slot, value, path, 0);
	}

	public void setValueWithPath(int slot, JsonNode value, @Nullable Path<JsonNode> path, int frameSize) {
		if (slot < 0)
			return;
		@Var EvaluationFrame<JsonNode> frame = evaluationFrame;
		if (frame == null) {
			EvaluationFrame<JsonNode> parentFrame = parentScope != null ? parentScope.getEvaluationFrame() : null;
			int size = Math.max(slot + 1, frameSize);
			frame = new EvaluationFrame<>(parentFrame, size);
			evaluationFrame = frame;
		}
		frame.setValueWithPath(slot, value, path);
	}

	public void setImportedData(String name, JsonNode data) {
		if (importedData == null)
			importedData = new HashMap<>();
		importedData.put(name, data);
	}

	public @Nullable JsonNode getImportedData(String name) {
		if (importedData != null) {
			JsonNode data = importedData.get(name);
			if (data != null)
				return data;
		}
		if (parentScope == null)
			return null;
		return parentScope.getImportedData(name);
	}

	public void addImportedModule(@Nullable String name, Module module) {
		if (importedModules == null)
			importedModules = new HashMap<>();
		importedModules.computeIfAbsent(name, (dummy) -> new LinkedList<>()).addFirst(module);
	}

	public List<Module> getImportedModules(@Nullable String name) { // the last import comes first
		List<Module> modules = new ArrayList<>();
		getImportedModules(modules, name);
		return modules;
	}

	private void getImportedModules(List<Module> modules, @Nullable String name) {
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

	public @Nullable Module getCurrentModule() {
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
