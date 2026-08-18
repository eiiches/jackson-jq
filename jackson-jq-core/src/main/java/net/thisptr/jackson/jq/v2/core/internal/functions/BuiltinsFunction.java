package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "builtins", nargs = 0)
public class BuiltinsFunction implements FunctionFactory {
	@Override
	public <JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		List<String> builtins = new ArrayList<>();
		for (FunctionNameAndArity fn : ClassPathFunctionLoader.getInstance().listFunctionFactories(version).keySet()) {
			builtins.add(fn.toString());
		}
		Collections.sort(builtins);

		return (scope, in, path, output, ignoredRequirePath) -> {
			JsonNode result = jsonProvider.createArray();
			for (String builtin : builtins)
				jsonProvider.add(result, jsonProvider.createString(builtin));
			output.emit(result, null);
		};
	}
}
