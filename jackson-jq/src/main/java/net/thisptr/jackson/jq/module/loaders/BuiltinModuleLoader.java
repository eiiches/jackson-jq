package net.thisptr.jackson.jq.module.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.annotations.Experimental;
import net.thisptr.jackson.jq.module.BuiltinModule;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

// DefaultModuleLoader uses ServiceLoader to load Module implementations from classpath
@Experimental
public class BuiltinModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final Map<String, Module<JsonNode>> pathAndModules = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static final BuiltinModuleLoader INSTANCE = new BuiltinModuleLoader(Module.class.getClassLoader());

	@SuppressWarnings("unchecked")
	public static <JsonNode> BuiltinModuleLoader<JsonNode> getInstance() {
		return (BuiltinModuleLoader<JsonNode>) INSTANCE;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public BuiltinModuleLoader(final ClassLoader classLoader) {
		for (final Module module : ServiceLoader.load(Module.class, classLoader)) {
			final BuiltinModule annotation = module.getClass().getAnnotation(BuiltinModule.class);
			if (annotation == null)
				continue;
			pathAndModules.put(annotation.path(), module);
		}
	}

	private boolean hasSearchPathOverride(final JsonProvider<JsonNode> jsonProvider, final JsonNode metadata) {
		if (metadata != null) {
			final JsonNode search = jsonProvider.get(metadata, "search");
			if (search != null)
				return true;
		}
		return false;
	}

	@Override
	public Module<JsonNode> loadModule(final Module<JsonNode> caller, final String path, final JsonNode metadata) throws JsonQueryException {
		// Note: we can't get jsonProvider here without having access to scope
		// For now, assume metadata checking for hasSearchPathOverride is handled by other loaders
		return pathAndModules.get(path);
	}

	@Override
	public JsonNode loadData(final Module<JsonNode> caller, final String path, final JsonNode metadata) {
		return null;
	}

	public Map<String, Module<JsonNode>> loadAllModules() {
		return new HashMap<>(pathAndModules);
	}
}
