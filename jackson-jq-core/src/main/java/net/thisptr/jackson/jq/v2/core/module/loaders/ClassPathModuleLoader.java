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

	public ClassPathModuleLoader(final ClassLoader classLoader) {
		for (final Module module : ServiceLoader.load(Module.class, classLoader)) {
			final ModuleRegistration annotation = module.getClass().getAnnotation(ModuleRegistration.class);
			if (annotation == null)
				continue;
			pathAndModules.put(annotation.path(), module);
		}
	}

	@Override
	public Module loadModule(final Module caller, final String path, final JsonNode metadata) throws JsonQueryException {
		// Note: we can't get jsonProvider here without having access to scope
		// For now, assume metadata checking for hasSearchPathOverride is handled by other loaders
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
