package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
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
	public @Nullable Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		// Note: we can't get jsonProvider here without having access to scope
		// For now, assume metadata checking for hasSearchPathOverride is handled by other loaders
		return pathAndModules.get(path);
	}

	@Override
	public Maybe<JsonNode> loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
		return Maybe.absent();
	}

	public Map<String, Module> loadAllModules() {
		return new HashMap<>(pathAndModules);
	}
}
