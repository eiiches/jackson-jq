package net.thisptr.jackson.jq.v2.spi.module;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
public interface Module<JsonNode> {
	Function<JsonNode> getFunction(String fname, int nargs);

	Map<String, Function<JsonNode>> getAllFunctions();
}
