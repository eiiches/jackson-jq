package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugExprFunction;
import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugScopeFunction;
import net.thisptr.jackson.jq.v2.ext.debug.functions.DumpExprFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/debug")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("debug_scope", 0), new DebugScopeFunction());
		functions.put(FunctionSignature.of("debug_expr", 1), new DebugExprFunction());
		functions.put(FunctionSignature.of("dump_expr", 1), new DumpExprFunction());
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
