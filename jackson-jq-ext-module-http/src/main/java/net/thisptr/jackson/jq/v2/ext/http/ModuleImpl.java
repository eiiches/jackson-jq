package net.thisptr.jackson.jq.v2.ext.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.http.functions.HttpGetFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/http")
public class ModuleImpl implements JavaModule {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("get", 1), new HttpGetFunction());
		functions.put(FunctionSignature.of("get", 2), new HttpGetFunction());
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
