package net.thisptr.jackson.jq.v2.ext.uri;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.uri.functions.UriDecodeFunction;
import net.thisptr.jackson.jq.v2.ext.uri.functions.UriParseFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uri")
public class ModuleImpl implements Module {
	private final Map<String, Function> functions = new HashMap<>();

	public ModuleImpl() {
		functions.put("uridecode/0", new UriDecodeFunction());
		functions.put("uriparse/0", new UriParseFunction());
	}

	@Override
	public @Nullable Function getFunction(String fname, int nargs) {
		return functions.get(fname + "/" + nargs);
	}

	@Override
	public Map<String, Function> getAllFunctions() {
		return Collections.unmodifiableMap(functions);
	}
}
