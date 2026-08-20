package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.debug.functions.DebugScopeFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/debug")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private final ModuleMeta moduleMeta;

	public ModuleImpl() {
		functions.put(FunctionSignature.of("debug_scope", 0), new DebugScopeFunction());
		this.moduleMeta = () -> Collections.unmodifiableList(new ArrayList<>(functions.keySet()));
	}

	@Override
	public @Nullable Function resolveFunction(String fname, int nargs) {
		return functions.get(FunctionSignature.of(fname, nargs));
	}

	@Override
	public ModuleMeta getModuleMeta() {
		return moduleMeta;
	}
}
