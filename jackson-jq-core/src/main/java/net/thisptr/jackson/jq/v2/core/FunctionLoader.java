package net.thisptr.jackson.jq.v2.core;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;

@FunctionalInterface
public interface FunctionLoader {
	Map<FunctionSignature, Function> listFunctions(Version version);
}
