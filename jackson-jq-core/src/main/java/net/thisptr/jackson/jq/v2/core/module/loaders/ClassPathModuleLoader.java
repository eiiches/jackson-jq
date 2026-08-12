package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.spi.module.ModuleRegistration;

// ClassPathModuleLoader uses ServiceLoader to load Module implementations from classpath
public class ClassPathModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final Map<String, Module> pathAndModules = new HashMap<>();

	@SuppressWarnings({ "rawtypes" })
	private static final ClassPathModuleLoader INSTANCE = new ClassPathModuleLoader(Module.class.getClassLoader());

	@SuppressWarnings("unchecked")
	public static <JsonNode> ClassPathModuleLoader<JsonNode> getInstance() {
		return (ClassPathModuleLoader<JsonNode>) INSTANCE;
	}

	public ClassPathModuleLoader(ClassLoader classLoader) {
		for (Module module : ServiceLoader.load(Module.class, classLoader)) {
			ModuleRegistration annotation = module.getClass().getAnnotation(ModuleRegistration.class);
			if (annotation == null)
				continue;
			pathAndModules.put(annotation.path(), module);
		}
	}

	@Override
	public Module loadModule(Module caller, String path, JsonNode metadata) throws JsonQueryException {
		// Note: we can't get jsonProvider here without having access to scope
		// For now, assume metadata checking for hasSearchPathOverride is handled by other loaders
		return pathAndModules.get(path);
	}

	@Override
	public JsonNode loadData(Module caller, String path, JsonNode metadata) {
		return null;
	}

	public Map<String, Module> loadAllModules() {
		return new HashMap<>(pathAndModules);
	}
}
