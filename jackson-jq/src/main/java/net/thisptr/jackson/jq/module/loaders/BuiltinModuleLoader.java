package net.thisptr.jackson.jq.module.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.BuiltinModule;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

// DefaultModuleLoader uses ServiceLoader to load Module implementations from classpath
public class BuiltinModuleLoader implements ModuleLoader {
	private final Map<String, Module> pathAndModules = new HashMap<>();

	private static final BuiltinModuleLoader INSTANCE = new BuiltinModuleLoader(Module.class.getClassLoader());

	public static BuiltinModuleLoader getInstance() {
		return INSTANCE;
	}

	public BuiltinModuleLoader(final ClassLoader classLoader) {
		for (final Module module : ServiceLoader.load(Module.class, classLoader)) {
			final BuiltinModule annotation = module.getClass().getAnnotation(BuiltinModule.class);
			if (annotation == null)
				continue;
			pathAndModules.put(annotation.path(), module);
		}
	}

	private static boolean hasSearchPathOverride(final JsonNode metadata) {
		if (metadata != null) {
			final JsonNode search = metadata.get("search");
			if (search != null)
				return true;
		}
		return false;
	}

	@Override
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		if (hasSearchPathOverride(metadata))
			return null;
		return pathAndModules.get(path);
	}

	@Override
	public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) {
		return null;
	}

	public Map<String, Module> loadAllModules() {
		return new HashMap<>(pathAndModules);
	}
}
