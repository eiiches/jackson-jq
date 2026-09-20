package net.thisptr.jackson.jq.v2.ext.time;

import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.time.functions.StrFTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.StrPTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.TimestampFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/time")
public final class ModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("strftime", 1), new StrFTimeFunction(),
			FunctionSignature.of("strftime", 2), new StrFTimeFunction(),
			FunctionSignature.of("strptime", 1), new StrPTimeFunction(),
			FunctionSignature.of("strptime", 2), new StrPTimeFunction(),
			FunctionSignature.of("timestamp", 0), new TimestampFunction());

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
