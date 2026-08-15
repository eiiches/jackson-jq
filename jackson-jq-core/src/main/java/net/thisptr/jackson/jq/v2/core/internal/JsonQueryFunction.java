package net.thisptr.jackson.jq.v2.core.internal;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class JsonQueryFunction<JsonNode> implements FunctionFactory {
	private Expression body;
	private List<String> params;
	private String name;
	private @Nullable Scope<JsonNode> closure;

	public JsonQueryFunction(String name, List<String> params, Expression body, @Nullable Scope<JsonNode> closure) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.closure = closure;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
		return (scope, in, path, output) -> {
			Preconditions.checkArgumentCount(name, args, params.size());

			Scope<JsonNode> parentScope = closure != null ? closure : (Scope) scope;
			Scope<JsonNode> fnScope = Scope.newChildScope(parentScope);
			fnScope.addFunctionFactory(name, params.size(), (FunctionFactory) JsonQueryFunction.this);

			pathRecursive((PathOutput) output, fnScope, (Scope) scope, args, (JsonNode) in, (Path) path, 0);
		};
	}

	private void pathRecursive(PathOutput<JsonNode> output, Scope<JsonNode> fnScope, Scope<JsonNode> callerScope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> path, int i) throws JsonQueryException {
		if (i == params.size()) {
			body.apply(fnScope, in, path, output, false);
		} else {
			String param = params.get(i);
			if (param.startsWith("$")) {
				String argname = param.substring(1);
				args.get(i).apply(callerScope, in, path, (argvalue, argpath) -> {
					fnScope.setValueWithPath(argname, argvalue, argpath);
					pathRecursive(output, fnScope, callerScope, args, in, path, i + 1);
				}, false);
			} else {
				fnScope.addFunctionFactory(param, 0, new JsonQueryFunction<>(param, Collections.emptyList(), new FixedScopeQuery<>(callerScope, args.get(i)), callerScope));
				pathRecursive(output, fnScope, callerScope, args, in, path, i + 1);
			}
		}
	}
}
