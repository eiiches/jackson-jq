package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class ArrayConstruction<JsonNode> implements Expression<JsonNode> {
	public final Expression<JsonNode> q;

	public ArrayConstruction() {
		this(null);
	}

	public ArrayConstruction(final Expression<JsonNode> q) {
		this.q = q;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNode[] array = (JsonNode[]) new Object[] { jsonProvider.createArray() };
		if (q != null)
			q.apply(scope, in, (out) -> array[0] = jsonProvider.add(array[0], out));
		output.emit(array[0], null);
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
