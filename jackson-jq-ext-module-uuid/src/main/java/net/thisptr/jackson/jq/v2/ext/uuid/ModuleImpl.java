package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid35Function;
import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid4Function;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleRegistration;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uuid")
public class ModuleImpl implements Module {
	private final Map<String, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("uuid4/0", new Uuid4Function());
		functions.put("uuid3/1", new Uuid35Function(3));
		functions.put("uuid5/1", new Uuid35Function(5));
	}

	@Override
	public Function getFunction(String fname, int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
