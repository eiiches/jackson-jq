package net.thisptr.jackson.jq.v2.ext.uri;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.ext.uri.functions.UriDecodeFunction;
import net.thisptr.jackson.jq.v2.ext.uri.functions.UriParseFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

@AutoService(Module.class)
@ModuleRegistration(path = "jackson-jq/uri")
public class ModuleImpl implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private final ModuleMeta moduleMeta;

	public ModuleImpl() {
		functions.put(FunctionSignature.of("uridecode", 0), new UriDecodeFunction());
		functions.put(FunctionSignature.of("uriparse", 0), new UriParseFunction());
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
