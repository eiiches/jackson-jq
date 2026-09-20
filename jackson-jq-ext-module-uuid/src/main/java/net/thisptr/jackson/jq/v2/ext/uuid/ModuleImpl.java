package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid35Function;
import net.thisptr.jackson.jq.v2.ext.uuid.functions.Uuid4Function;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/uuid")
public final class ModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("uuid4", 0), new Uuid4Function(),
			FunctionSignature.of("uuid3", 1), new Uuid35Function(3),
			FunctionSignature.of("uuid5", 1), new Uuid35Function(5));

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
