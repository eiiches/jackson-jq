package net.thisptr.jackson.jq.internal;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

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
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkArgumentCount(name, args, params.size());

		final Scope fnScope = Scope.newChildScope(closure);
		fnScope.addFunction(name, params.size(), this);

		applyRecursive(output, fnScope, scope, args, in, 0);
	}

	private void applyRecursive(final Output output, final Scope fnScope, final Scope scope, final List<Expression> args, final JsonNode in, final int i) throws JsonQueryException {
		if (i == params.size()) {
			body.apply(fnScope, in, output);
		} else {
			final String param = params.get(i);
			if (param.startsWith("$")) {
				final String argname = param.substring(1);
				args.get(i).apply(scope, in, (argvalue) -> {
					fnScope.setValue(argname, argvalue);
					applyRecursive(output, fnScope, scope, args, in, i + 1);
				});
			} else {
				fnScope.addFunction(param, 0, new JsonQueryFunction(param, Collections.<String>emptyList(), new FixedScopeQuery(scope, args.get(i)), fnScope));
				applyRecursive(output, fnScope, scope, args, in, i + 1);
			}
		}
	}
}
