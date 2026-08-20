package net.thisptr.jackson.jq.v2.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * A {@link FunctionLoader} that memoizes {@link #getFunctions(Version)} results per {@link Version},
 * so a delegate that does expensive work (e.g. classpath scanning) only pays that cost once.
 */
public class CachedFunctionLoader implements FunctionLoader {
	private final FunctionLoader delegate;
	private final ConcurrentHashMap<Version, Map<FunctionSignature, Function>> cache = new ConcurrentHashMap<>();

	public CachedFunctionLoader(FunctionLoader delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions(Version version) {
		return cache.computeIfAbsent(version, delegate::getFunctions);
	}
}
