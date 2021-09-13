package net.thisptr.jackson.jq.internal;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public class JsonQueryFunction implements Function {
	private Expression body;
	private List<String> params;
	private String name;
	private Scope closure;

	public JsonQueryFunction(final String name, final List<String> params, final Expression body, final Scope closure) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.closure = closure;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkArgumentCount(name, args, params.size());

		final Scope fnScope = Scope.newChildScope(closure);
		fnScope.addFunction(name, params.size(), this);

		pathRecursive(output, fnScope, scope, args, in, path, 0);
	}

	private void pathRecursive(final PathOutput output, final Scope fnScope, final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final int i) throws JsonQueryException {
		if (i == params.size()) {
			body.apply(fnScope, in, path, output, false);
		} else {
			final String param = params.get(i);
			if (param.startsWith("$")) {
				final String argname = param.substring(1);
				args.get(i).apply(scope, in, path, (argvalue, argpath) -> {
					fnScope.setValueWithPath(argname, argvalue, argpath);
					pathRecursive(output, fnScope, scope, args, in, path, i + 1);
				}, false);
			} else {
				fnScope.addFunction(param, 0, new JsonQueryFunction(param, Collections.emptyList(), new FixedScopeQuery(scope, args.get(i)), fnScope));
				pathRecursive(output, fnScope, scope, args, in, path, i + 1);
			}
		}
	}
}
