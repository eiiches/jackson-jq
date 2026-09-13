package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

// ClassPathModuleLoader uses ServiceLoader to load Module implementations from classpath
public class ClassPathModuleLoader<JsonNode> implements ModuleLoader<JsonNode> {
	private final Map<String, Module> pathAndModules = new HashMap<>();

	private static final ClassPathModuleLoader<?> INSTANCE = new ClassPathModuleLoader<>(Module.class.getClassLoader());

	@SuppressWarnings("unchecked")
	public static <JsonNode> ClassPathModuleLoader<JsonNode> getInstance() {
		return (ClassPathModuleLoader<JsonNode>) INSTANCE;
	}

	/**
	 * If two modules register the same {@link ModuleRegistration#path()}, which one is used is
	 * unspecified: this loader currently resolves the collision by {@link ServiceLoader} discovery
	 * order, but that order itself is not part of this loader's contract.
	 */
	public ClassPathModuleLoader(ClassLoader classLoader) {
		for (Module module : ServiceLoader.load(Module.class, classLoader)) {
			for (ModuleRegistration annotation : module.getClass().getAnnotationsByType(ModuleRegistration.class)) {
				pathAndModules.put(annotation.path(), module);
			}
		}
	}

	@Override
	public Module loadModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		Module module = pathAndModules.get(path);
		if (module == null)
			throw new ModuleNotFoundException(path);
		return module;
	}

	// Registered modules are Java objects, not files; this loader has no data files to serve.
	@Override
	public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
		throw new ModuleNotFoundException(path);
	}
}
