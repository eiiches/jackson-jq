package net.thisptr.jackson.jq.v2.ext.time;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.time.functions.StrFTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.StrPTimeFunction;
import net.thisptr.jackson.jq.v2.ext.time.functions.TimestampFunction;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/time")
public class ModuleImpl implements Module {
	private final Map<String, FunctionFactory> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("strftime/1", new StrFTimeFunction());
		functions.put("strftime/2", new StrFTimeFunction());
		functions.put("strptime/1", new StrPTimeFunction());
		functions.put("strptime/2", new StrPTimeFunction());
		functions.put("timestamp/0", new TimestampFunction());
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
