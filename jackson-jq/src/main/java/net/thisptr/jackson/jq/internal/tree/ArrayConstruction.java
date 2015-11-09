package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ArrayConstruction extends JsonQuery {

	private JsonQuery q;

	public ArrayConstruction() {
		this(null);
	}

	public ArrayConstruction(final JsonQuery q) {
		this.q = q;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final ArrayNode array = new ArrayNode(scope.getObjectMapper().getNodeFactory());
		if (q != null)
			array.addAll(q.apply(scope, in));
		return Collections.singletonList((JsonNode) array);
	}

	@Override
	public String toString() {
		if (q == null)
			return String.format("[]");
		return String.format("[%s]", q);
	}
}
