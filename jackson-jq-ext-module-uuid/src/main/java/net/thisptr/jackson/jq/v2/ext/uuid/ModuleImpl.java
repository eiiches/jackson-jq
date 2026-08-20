package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid35Function;
import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid4Function;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uuid")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("uuid4", 0), new Uuid4Function());
		functions.put(FunctionSignature.of("uuid3", 1), new Uuid35Function(3));
		functions.put(FunctionSignature.of("uuid5", 1), new Uuid35Function(5));
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
