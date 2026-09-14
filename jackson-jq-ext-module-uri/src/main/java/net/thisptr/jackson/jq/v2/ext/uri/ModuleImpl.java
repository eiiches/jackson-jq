package net.thisptr.jackson.jq.v2.ext.uri;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.uri.functions.UriDecodeFunction;
import net.thisptr.jackson.jq.v2.ext.uri.functions.UriParseFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/uri")
public class ModuleImpl implements JavaModule {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("uridecode", 0), new UriDecodeFunction());
		functions.put(FunctionSignature.of("uriparse", 0), new UriParseFunction());
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
