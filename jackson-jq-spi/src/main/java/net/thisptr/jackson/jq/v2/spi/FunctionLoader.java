package net.thisptr.jackson.jq.v2.spi;

import java.util.Map;

@FunctionalInterface
public interface FunctionLoader {
	Map<FunctionNameAndArity, Function> listFunctions(Version version);
}
