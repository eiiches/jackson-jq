package net.thisptr.jackson.jq.v2.ext.joni;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

/**
 * The regex primitives {@link JoniRegexModule}'s jq source calls.
 * <p>
 * This class exists only to answer the {@code jackson-jq/joni/_impl} import path and is not part of
 * the supported API; the functions it exports are spelt with a leading underscore because nothing
 * outside that source should call them.
 */
@ModuleRegistration(path = "jackson-jq/joni/_impl")
public final class InternalModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("_match_impl", 3), new MatchImplFunction(),
			FunctionSignature.of("_sub_impl", 3), new SubImplFunction());

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
