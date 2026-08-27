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

	/**
	 * Loads Java-implemented functions visible to this loader's {@link ClassLoader}.
	 * <p>
	 * If two providers register the same {@link FunctionSignature}, which one is used is
	 * unspecified: this loader currently resolves the collision by {@link ServiceLoader} discovery
	 * order, but that order itself is not part of this loader's contract.
	 */
	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		Map<FunctionSignature, Function> result = new HashMap<>();

		for (Function factory : ServiceLoader.load(Function.class, classLoader)) {
			FunctionRegistration[] regs = factory.getClass().getAnnotationsByType(FunctionRegistration.class);
			for (FunctionRegistration reg : regs) {
				VersionRange versionRange = VersionRange.from(reg.version());
				if (!versionRange.contains(jqVersion))
					continue;

				result.put(signatureOf(reg), factory);
			}
		}

		return result;
	}

	/** A negative {@link FunctionRegistration#nargs()} registers a variadic function, matching {@link FunctionSignature}'s null-arity convention. */
	static FunctionSignature signatureOf(FunctionRegistration reg) {
		return reg.nargs() < 0 ? FunctionSignature.ofVariadic(reg.name()) : FunctionSignature.of(reg.name(), reg.nargs());
	}

	/**
	 * Loads raw jq function definitions visible to this loader's {@link ClassLoader}.
	 * <p>
	 * If two {@link JqLibrary}s register the same {@link FunctionSignature} for an overlapping
	 * version range, which one is used is unspecified, for the same reason as
	 * {@link #getFunctions(Version)}.
	 */
	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
		Map<FunctionSignature, JqFunction> result = new HashMap<>();

		for (JqLibrary library : ServiceLoader.load(JqLibrary.class, classLoader)) {
			for (JqFunction def : library.getJqFunctions()) {
				VersionRange version = def.version();
				if (version != null && !version.contains(jqVersion))
					continue;
				result.put(def.signature(), def);
			}
		}

		return result;
	}
}
