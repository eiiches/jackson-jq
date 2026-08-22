package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "builtins", nargs = 0)
public class BuiltinsFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		List<String> builtins = new ArrayList<>();
		Set<FunctionSignature> signatures = new HashSet<>(ClassPathFunctionLoader.getInstance().getFunctions(version).keySet());
		signatures.addAll(ClassPathFunctionLoader.getInstance().getJqFunctions(version).keySet());
		for (FunctionSignature fn : signatures) {
			builtins.add(fn.toString());
		}
		Collections.sort(builtins);

		return FunctionBody.builder(args).cardinality(Cardinality.ONE).build((scope, in, path, output) -> {
			JsonNode result = jsonProvider.createArray();
			for (String builtin : builtins)
				jsonProvider.add(result, jsonProvider.createString(builtin));
			output.emit(result, null);
		});
	}
}
