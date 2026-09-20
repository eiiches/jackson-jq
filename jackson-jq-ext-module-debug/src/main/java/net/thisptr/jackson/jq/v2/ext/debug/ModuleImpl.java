package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugExprFunction;
import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugScopeFunction;
import net.thisptr.jackson.jq.v2.ext.debug.functions.DumpExprFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/debug")
public final class ModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("debug_scope", 0), new DebugScopeFunction(),
			FunctionSignature.of("debug_expr", 1), new DebugExprFunction(),
			FunctionSignature.of("dump_expr", 1), new DumpExprFunction());

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
