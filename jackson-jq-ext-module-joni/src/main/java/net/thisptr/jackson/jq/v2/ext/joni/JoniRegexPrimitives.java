package net.thisptr.jackson.jq.v2.ext.joni;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

final class JoniRegexPrimitives implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("_match_impl", 3), new MatchImplFunction(),
			FunctionSignature.of("_sub_impl", 3), new SubImplFunction());

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
