package net.thisptr.jackson.jq.v2.ext.gzip;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/gzip")
public final class ModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("compress_binary", 0), new GzipFunction("compress_binary", true, false),
			FunctionSignature.of("compress_text", 0), new GzipFunction("compress_text", true, true),
			FunctionSignature.of("compress_text", 1), new GzipFunction("compress_text", true, true),
			FunctionSignature.of("decompress_binary", 0), new GzipFunction("decompress_binary", false, false),
			FunctionSignature.of("decompress_text", 0), new GzipFunction("decompress_text", false, true),
			FunctionSignature.of("decompress_text", 1), new GzipFunction("decompress_text", false, true));

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
