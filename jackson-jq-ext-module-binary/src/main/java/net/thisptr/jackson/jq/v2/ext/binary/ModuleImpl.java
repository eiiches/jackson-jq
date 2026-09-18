package net.thisptr.jackson.jq.v2.ext.binary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/binary")
public class ModuleImpl implements JavaModule {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		DecodeTextFunction decodeText = new DecodeTextFunction();
		functions.put(FunctionSignature.of("decode_text", 0), decodeText);
		functions.put(FunctionSignature.of("decode_text", 1), decodeText);

		EncodeTextFunction encodeText = new EncodeTextFunction();
		functions.put(FunctionSignature.of("encode_text", 0), encodeText);
		functions.put(FunctionSignature.of("encode_text", 1), encodeText);
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
