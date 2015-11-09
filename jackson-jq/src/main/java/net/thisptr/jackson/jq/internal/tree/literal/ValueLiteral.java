package net.thisptr.jackson.jq.internal.tree.literal;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class ValueLiteral extends JsonQuery {
	private JsonNode value;

	public ValueLiteral(final JsonNode value) {
		this.value = value;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) {
		return Collections.singletonList(value);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
