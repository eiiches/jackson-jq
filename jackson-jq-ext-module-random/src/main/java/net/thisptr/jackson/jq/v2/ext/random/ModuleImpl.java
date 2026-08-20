package net.thisptr.jackson.jq.v2.ext.random;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.random.functions.RandomFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/random")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("random", 0), new RandomFunction());
	}

	@Override
	public @Nullable Function resolveFunction(String fname, int nargs) {
		return functions.get(FunctionSignature.of(fname, nargs));
	}

	@Override
	public Map<FunctionSignature, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
