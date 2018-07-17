package net.thisptr.jackson.jq;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.IsolatedScopeQuery;
import net.thisptr.jackson.jq.internal.javacc.JsonQueryParser;
import net.thisptr.jackson.jq.internal.javacc.TokenMgrError;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class JsonQuery {
	@Deprecated
	private static Scope defaultScope = new Scope();

	public abstract List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException;

	@Deprecated
	public List<JsonNode> apply(final JsonNode in) throws JsonQueryException {
		return apply(defaultScope, in);
	}

	public List<JsonNode> apply(final Scope scope, final List<JsonNode> in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode i : in)
			out.addAll(apply(scope, i));
		return out;
	}

	@Deprecated
	public List<JsonNode> apply(final List<JsonNode> in) throws JsonQueryException {
		return apply(defaultScope, in);
	}

	public static JsonQuery compile(final String path) throws JsonQueryException {
		try {
			return new IsolatedScopeQuery(JsonQueryParser.compile(path));
		} catch (final TokenMgrError | Exception e) {
			throw new JsonQueryException(e);
		}
	}
}
