package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugScopeFunction;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/debug")
public class ModuleImpl implements Module {
	private final Map<String, FunctionFactory> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("debug_scope/0", new DebugScopeFunction());
	}

	@Override
	public @Nullable FunctionFactory getFunction(String fname, int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, FunctionFactory> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
