package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("builtins/0")
public class BuiltinsFunction implements Function {

	@Override
	public void apply(Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output, final Version version) throws JsonQueryException {
		// root scope
		while (scope.getParentScope() != null)
			scope = scope.getParentScope();

		final List<String> builtins = new ArrayList<>(scope.getLocalFunctions().keySet());
		Collections.sort(builtins);
		output.emit(scope.getObjectMapper().valueToTree(builtins), null);
	}
}
