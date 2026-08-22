package net.thisptr.jackson.jq.v2.core.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.thisptr.jackson.jq.v2.core.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * A {@link FunctionLoader} that memoizes both function registries per {@link Version}, so a delegate
 * that does expensive work (e.g. classpath scanning) only pays that cost once.
 */
public class CachedFunctionLoader implements FunctionLoader {
	private final FunctionLoader delegate;
	private final ConcurrentHashMap<Version, Map<FunctionSignature, Function>> cache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Version, Map<FunctionSignature, JqFunction>> jqCache = new ConcurrentHashMap<>();

	public CachedFunctionLoader(FunctionLoader delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		return cache.computeIfAbsent(jqVersion, delegate::getFunctions);
	}

	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
		return jqCache.computeIfAbsent(jqVersion, delegate::getJqFunctions);
	}
}
