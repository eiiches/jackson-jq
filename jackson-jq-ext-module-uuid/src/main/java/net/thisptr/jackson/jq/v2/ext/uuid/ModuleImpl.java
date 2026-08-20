package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid35Function;
import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid4Function;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uuid")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private final ModuleMeta moduleMeta;

	public ModuleImpl() {
		functions.put(FunctionSignature.of("uuid4", 0), new Uuid4Function());
		functions.put(FunctionSignature.of("uuid3", 1), new Uuid35Function(3));
		functions.put(FunctionSignature.of("uuid5", 1), new Uuid35Function(5));
		this.moduleMeta = new ModuleMeta() {};
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}

	@Override
	public ModuleMeta getModuleMeta() {
		return moduleMeta;
	}
}
