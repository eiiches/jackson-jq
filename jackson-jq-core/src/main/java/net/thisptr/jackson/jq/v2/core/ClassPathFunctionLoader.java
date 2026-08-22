package net.thisptr.jackson.jq.v2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.VersionRange;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

/** Loads Java functions and raw jq function definitions through {@link ServiceLoader}. */
public class ClassPathFunctionLoader implements FunctionLoader {
	private static final ClassPathFunctionLoader INSTANCE = new ClassPathFunctionLoader(ClassPathFunctionLoader.class.getClassLoader());

	private final ClassLoader classLoader;

	public static ClassPathFunctionLoader getInstance() {
		return INSTANCE;
	}

	public ClassPathFunctionLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/** Loads Java-implemented functions visible to this loader's {@link ClassLoader}. */
	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		Map<FunctionSignature, Function> result = new HashMap<>();

		for (Function factory : ServiceLoader.load(Function.class, classLoader)) {
			FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
			for (FunctionRegistration reg : regs) {
				VersionRange versionRange = VersionRange.valueOf(reg.version());
				if (!versionRange.contains(jqVersion))
					continue;

				result.put(FunctionSignature.of(reg.name(), reg.nargs()), factory);
			}
		}

		return result;
	}

	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
		Map<FunctionSignature, JqFunction> result = new HashMap<>();

		for (JqLibrary library : ServiceLoader.load(JqLibrary.class, classLoader)) {
			for (JqFunction def : library.getJqFunctions()) {
				if (def.version != null && !def.version.contains(jqVersion))
					continue;
				result.put(FunctionSignature.of(def.name, def.args.size()), def);
			}
		}

		return result;
	}
}
