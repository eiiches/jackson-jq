package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/re2/_impl")
public final class InternalModuleImpl implements JavaModule {
	private final Map<FunctionSignature, Function> functions;

	public InternalModuleImpl() {
		Map<FunctionSignature, Function> result = new HashMap<>();
		result.put(FunctionSignature.of("_match_impl", 3), new MatchImplFunction());
		result.put(FunctionSignature.of("_sub_impl", 3), new SubImplFunction());
		functions = Collections.unmodifiableMap(result);
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return functions;
	}
}
