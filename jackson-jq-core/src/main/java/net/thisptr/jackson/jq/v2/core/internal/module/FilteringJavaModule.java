package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.thisptr.jackson.jq.v2.core.function.FunctionContext;
import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.core.function.FunctionOrigin;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

/**
 * A {@link JavaModule} view that exports only the functions of its delegate accepted by a
 * {@link FunctionFilter}. The module metadata is the delegate's, unchanged.
 */
public final class FilteringJavaModule implements JavaModule {
	private final JavaModule delegate;
	private final Map<FunctionSignature, Function> functions;

	private FilteringJavaModule(JavaModule delegate, Map<FunctionSignature, Function> functions) {
		this.delegate = delegate;
		this.functions = functions;
	}

	/**
	 * Returns {@code module} restricted to the functions {@code filter} accepts, or {@code module}
	 * itself if the filter accepts everything.
	 *
	 * @param module the module to filter
	 * @param filter the function filter to apply
	 * @param origin the origin describing how the module entered the system
	 * @param locator the string identifying the specific module within its origin
	 * @return the filtered module, or {@code module} if the filter accepts all functions
	 */
	public static JavaModule of(JavaModule module, FunctionFilter filter, FunctionOrigin origin, String locator) {
		Objects.requireNonNull(module, "module");
		Objects.requireNonNull(filter, "filter");
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(locator, "locator");
		if (filter == FunctionFilter.acceptAll())
			return module;
		Map<FunctionSignature, Function> accepted = new LinkedHashMap<>();
		for (Map.Entry<FunctionSignature, Function> entry : module.getFunctions().entrySet()) {
			if (filter.accept(FunctionContext.of(entry.getKey(), origin, locator)))
				accepted.put(entry.getKey(), entry.getValue());
		}
		return new FilteringJavaModule(module, Collections.unmodifiableMap(accepted));
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return functions;
	}

	@Override
	public ModuleMeta getModuleMeta() {
		return delegate.getModuleMeta();
	}
}
