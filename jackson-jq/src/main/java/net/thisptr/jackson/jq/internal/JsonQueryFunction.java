package net.thisptr.jackson.jq.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonQueryFunction implements Function {
	private JsonQuery body;
	private List<String> params;
	private String name;
	private Scope closure;

	public JsonQueryFunction(final String name, final List<String> params, final JsonQuery body, final Scope closure) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.closure = closure;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkArgumentCount(name, args, params.size());

		final Scope fnScope = Scope.newChildScope(closure);
		fnScope.addFunction(name, params.size(), this);

		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(out, fnScope, scope, args, in, 0);
		return out;
	}

	private void applyRecursive(final List<JsonNode> out, final Scope fnScope, final Scope scope, final List<JsonQuery> args, final JsonNode in, final int i) throws JsonQueryException {
		if (i == params.size()) {
			out.addAll(body.apply(fnScope, in));
		} else {
			final String param = params.get(i);
			if (param.startsWith("$")) {
				final String argname = param.substring(1);
				for (final JsonNode argvalue : args.get(i).apply(scope, in)) {
					fnScope.setValue(argname, argvalue);
					applyRecursive(out, fnScope, scope, args, in, i + 1);
				}
			} else {
				fnScope.addFunction(param, 0, new JsonQueryFunction(param, Collections.<String> emptyList(), new FixedScopeQuery(scope, args.get(i)), fnScope));
				applyRecursive(out, fnScope, scope, args, in, i + 1);
			}
		}
	}
}
