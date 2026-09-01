package net.thisptr.jackson.jq.v2.ext.time;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.time.functions.StrFTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.StrPTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.TimestampFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/time")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put(FunctionSignature.of("strftime", 1), new StrFTimeFunction());
		functions.put(FunctionSignature.of("strftime", 2), new StrFTimeFunction());
		functions.put(FunctionSignature.of("strptime", 1), new StrPTimeFunction());
		functions.put(FunctionSignature.of("strptime", 2), new StrPTimeFunction());
		functions.put(FunctionSignature.of("timestamp", 0), new TimestampFunction());
	}

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
