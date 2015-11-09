package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;

public class NegativeExpression extends JsonQuery {
	private JsonQuery value;

	public NegativeExpression(final JsonQuery value) {
		this.value = value;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode i : value.apply(scope, in)) {
			if (!i.isNumber())
				throw new JsonQueryTypeException(in, "cannot be negated");
			out.add(JsonNodeUtils.asNumericNode(-i.asDouble()));
		}
		return out;
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
