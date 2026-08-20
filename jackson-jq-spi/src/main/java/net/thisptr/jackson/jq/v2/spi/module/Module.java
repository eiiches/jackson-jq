package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

public interface Module {
	@Nullable Function resolveFunction(String fname, int nargs);

	Map<FunctionSignature, Function> getAllFunctions();
}
