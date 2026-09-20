package net.thisptr.jackson.jq.v2.ext.fs;

import java.util.Map;

import net.thisptr.jackson.jq.v2.ext.fs.functions.FileReadFunction;
import net.thisptr.jackson.jq.v2.ext.fs.functions.FileWriteFunction;
import net.thisptr.jackson.jq.v2.ext.fs.functions.JsonReadFunction;
import net.thisptr.jackson.jq.v2.ext.fs.functions.JsonWriteFunction;
import net.thisptr.jackson.jq.v2.ext.fs.functions.ListFunction;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;

@ModuleRegistration(path = "jackson-jq/fs")
public final class ModuleImpl implements JavaModule {
	private static final Map<FunctionSignature, Function> FUNCTIONS = Map.ofEntries(
			Map.entry(FunctionSignature.of("read_text", 1), FileReadFunction.text()),
			Map.entry(FunctionSignature.of("read_text", 2), FileReadFunction.text()),
			Map.entry(FunctionSignature.of("read_binary", 1), FileReadFunction.binary()),
			Map.entry(FunctionSignature.of("write_text", 1), FileWriteFunction.text()),
			Map.entry(FunctionSignature.of("write_text", 2), FileWriteFunction.text()),
			Map.entry(FunctionSignature.of("write_binary", 1), FileWriteFunction.binary()),
			Map.entry(FunctionSignature.of("write_binary", 2), FileWriteFunction.binary()),
			Map.entry(FunctionSignature.of("list", 1), new ListFunction()),
			Map.entry(FunctionSignature.of("list", 2), new ListFunction()),
			Map.entry(FunctionSignature.of("read_json", 1), JsonReadFunction.single()),
			Map.entry(FunctionSignature.of("read_json", 2), JsonReadFunction.single()),
			Map.entry(FunctionSignature.of("read_json_stream", 1), JsonReadFunction.stream()),
			Map.entry(FunctionSignature.of("read_json_stream", 2), JsonReadFunction.stream()),
			Map.entry(FunctionSignature.of("write_json", 1), new JsonWriteFunction()),
			Map.entry(FunctionSignature.of("write_json", 2), new JsonWriteFunction()));

	@Override
	public Map<FunctionSignature, Function> getFunctions() {
		return FUNCTIONS;
	}
}
