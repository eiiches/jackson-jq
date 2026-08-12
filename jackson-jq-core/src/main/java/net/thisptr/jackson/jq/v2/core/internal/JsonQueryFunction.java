package net.thisptr.jackson.jq.v2.core.internal;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class JsonQueryFunction<JsonNode> implements Function {
	private Expression<JsonNode> body;
	private List<String> params;
	private String name;
	private Scope<JsonNode> closure;

	public JsonQueryFunction(String name, List<String> params, Expression<JsonNode> body, Scope<JsonNode> closure) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.closure = closure;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(Scope<N> scope, List<Expression<N>> args, N in, @Nullable Path<N> path, PathOutput<N> output, Version version) throws JsonQueryException {
		applyInternal((Scope) scope, (List) args, (JsonNode) in, (Path) path, (PathOutput) output, version);
	}

	private void applyInternal(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		Preconditions.checkArgumentCount(name, args, params.size());

		Scope<JsonNode> fnScope = Scope.newChildScope(closure);
		fnScope.addFunction(name, params.size(), this);

		pathRecursive(output, fnScope, scope, args, in, path, 0);
	}

	private void pathRecursive(PathOutput<JsonNode> output, Scope<JsonNode> fnScope, Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, @Nullable Path<JsonNode> path, int i) throws JsonQueryException {
		if (i == params.size()) {
			body.apply(fnScope, in, path, output, false);
		} else {
			String param = params.get(i);
			if (param.startsWith("$")) {
				String argname = param.substring(1);
				args.get(i).apply(scope, in, path, (argvalue, argpath) -> {
					fnScope.setValueWithPath(argname, argvalue, argpath);
					pathRecursive(output, fnScope, scope, args, in, path, i + 1);
				}, false);
			} else {
				fnScope.addFunction(param, 0, new JsonQueryFunction<>(param, Collections.emptyList(), new FixedScopeQuery<>(scope, args.get(i)), fnScope));
				pathRecursive(output, fnScope, scope, args, in, path, i + 1);
			}
		}
	}
}
