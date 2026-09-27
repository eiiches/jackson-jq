package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/binary")
public final class BinaryModule implements JavaModule {

	/**
	 * Required by ServiceLoader.
	 */
	public BinaryModule() {
	}

	private static final DecodeTextFunction DECODE_TEXT = new DecodeTextFunction();
	private static final EncodeTextFunction ENCODE_TEXT = new EncodeTextFunction();

	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.ofEntries(
			Map.entry(FunctionSignature.of("decode_text", 0), DECODE_TEXT),
			Map.entry(FunctionSignature.of("decode_text", 1), DECODE_TEXT),
			Map.entry(FunctionSignature.of("encode_text", 0), ENCODE_TEXT),
			Map.entry(FunctionSignature.of("encode_text", 1), ENCODE_TEXT),
			Map.entry(FunctionSignature.of("size", 0), new SizeFunction()),
			Map.entry(FunctionSignature.of("to_hex", 0), new ToHexFunction()),
			Map.entry(FunctionSignature.of("from_hex", 0), new FromHexFunction()),
			Map.entry(FunctionSignature.of("to_base64", 0), new ToBase64Function()),
			Map.entry(FunctionSignature.of("from_base64", 0), new FromBase64Function()),
			Map.entry(FunctionSignature.of("to_base64url", 0), new ToBase64UrlFunction()),
			Map.entry(FunctionSignature.of("from_base64url", 0), new FromBase64UrlFunction()),
			Map.entry(FunctionSignature.of("to_bytes", 0), new ToBytesFunction()),
			Map.entry(FunctionSignature.of("from_bytes", 0), new FromBytesFunction())
	);

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
