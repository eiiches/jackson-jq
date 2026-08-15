package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;

public interface Module {
	@Nullable FunctionFactory getFunction(String fname, int nargs);

	Map<String, FunctionFactory> getAllFunctions();
}
