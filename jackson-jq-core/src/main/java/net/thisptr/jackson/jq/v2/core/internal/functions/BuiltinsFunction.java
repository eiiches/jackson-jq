package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "builtins", nargs = 0)
public class BuiltinsFunction implements Function {

	@Override
	public <JsonNode> void apply(@Var Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		// root scope
		while (scope.getParentScope() != null)
			scope = scope.getParentScope();

		List<String> builtins = new ArrayList<>(scope.getLocalFunctions().keySet());
		Collections.sort(builtins);

		JsonNode result = jsonProvider.createArray();
		for (String builtin : builtins)
			jsonProvider.add(result, jsonProvider.createString(builtin));
		output.emit(result, null);
	}
}
