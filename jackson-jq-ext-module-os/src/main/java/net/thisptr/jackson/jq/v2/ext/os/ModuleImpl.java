package net.thisptr.jackson.jq.v2.ext.os;

import java.util.Map;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/os")
public final class ModuleImpl implements JavaModule {
	private static final HostnameFunction HOSTNAME = new HostnameFunction();
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.of(
			FunctionSignature.of("hostname", 0), HOSTNAME,
			FunctionSignature.of("hostname", 1), HOSTNAME);

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
