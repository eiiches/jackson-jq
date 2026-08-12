package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Function;

public interface Module {
	@Nullable Function getFunction(String fname, int nargs);

	Map<String, Function> getAllFunctions();
}
