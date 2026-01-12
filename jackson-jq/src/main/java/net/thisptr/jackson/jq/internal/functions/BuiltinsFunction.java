package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("builtins/0")
public class BuiltinsFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		// root scope
		while (scope.getParentScope() != null)
			scope = scope.getParentScope();

		final List<String> builtins = new ArrayList<>(scope.getLocalFunctions().keySet());
		Collections.sort(builtins);

		final JsonNode result = jsonProvider.createArray();
		for (final String builtin : builtins)
			jsonProvider.add(result, jsonProvider.createString(builtin));
		output.emit(result, null);
	}
}
