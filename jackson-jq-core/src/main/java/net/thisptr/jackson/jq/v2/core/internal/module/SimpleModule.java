package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

public class SimpleModule implements Module {
	private final Map<FunctionSignature, Function> functions = new HashMap<>();
	private ModuleMeta moduleMeta;

	public SimpleModule() {
		this.moduleMeta = new SimpleModuleMeta();
	}

	public SimpleModule(ModuleMeta moduleMeta) {
		this.moduleMeta = Objects.requireNonNull(moduleMeta);
	}

	public void setModuleMeta(ModuleMeta moduleMeta) {
		this.moduleMeta = Objects.requireNonNull(moduleMeta);
	}

	public void addFunction(String fname, int nargs, Function f) {
		addFunction(FunctionSignature.of(fname, nargs), f);
	}

	public void addFunction(FunctionSignature key, Function f) {
		functions.put(key, f);
	}

	public void addAllFunctions(Map<FunctionSignature, Function> functions) {
		this.functions.putAll(functions);
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
