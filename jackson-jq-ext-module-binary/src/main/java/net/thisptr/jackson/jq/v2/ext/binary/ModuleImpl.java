package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/binary")
public final class ModuleImpl implements JavaModule {
	private static final DecodeTextFunction DECODE_TEXT = new DecodeTextFunction();
	private static final EncodeTextFunction ENCODE_TEXT = new EncodeTextFunction();
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("decode_text", 0), DECODE_TEXT,
			FunctionSignature.of("decode_text", 1), DECODE_TEXT,
			FunctionSignature.of("encode_text", 0), ENCODE_TEXT,
			FunctionSignature.of("encode_text", 1), ENCODE_TEXT);

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
