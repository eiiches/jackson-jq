package net.thisptr.jackson.jq.v2.core.internal.function.loaders;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * A {@link FunctionLoader} that exposes only the functions of its delegate accepted by a {@link FunctionFilter}.
 *
 * <p>Both registries are filtered identically, and the filtered result is computed once per {@link Version}.</p>
 */
public class FilteringFunctionLoader implements FunctionLoader {
	private final FunctionLoader delegate;
	private final FunctionFilter filter;
	private final ConcurrentHashMap<Version, Map<FunctionSignature, Function>> cache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Version, Map<FunctionSignature, JqFunction>> jqCache = new ConcurrentHashMap<>();

	public FilteringFunctionLoader(FunctionLoader delegate, FunctionFilter filter) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
		this.filter = Objects.requireNonNull(filter, "filter");
	}

	public FunctionFilter getFilter() {
		return filter;
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions(Version jqVersion) {
		return cache.computeIfAbsent(jqVersion, v -> filter(delegate.getFunctions(v)));
	}

	@Override
	public Map<FunctionSignature, JqFunction> getJqFunctions(Version jqVersion) {
		return jqCache.computeIfAbsent(jqVersion, v -> filter(delegate.getJqFunctions(v)));
	}

	private <T> Map<FunctionSignature, T> filter(Map<FunctionSignature, T> functions) {
		Map<FunctionSignature, T> accepted = new LinkedHashMap<>();
		for (Map.Entry<FunctionSignature, T> entry : functions.entrySet()) {
			if (filter.accept(entry.getKey()))
				accepted.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(accepted);
	}
}
