package net.thisptr.jackson.jq.v2.ext.uri;

import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.uri.functions.UriDecodeFunction;
import net.thisptr.jackson.jq.v2.ext.uri.functions.UriParseFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/uri")
public final class UriModule implements JavaModule {

	/**
	 * Required by ServiceLoader.
	 */
	public UriModule() {
	}

	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("uridecode", 0), new UriDecodeFunction(),
			FunctionSignature.of("uriparse", 0), new UriParseFunction());

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
