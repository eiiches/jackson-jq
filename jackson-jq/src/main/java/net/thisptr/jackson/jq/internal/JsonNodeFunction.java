package net.thisptr.jackson.jq.internal;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeFunction implements Function {
	private JsonNode value;

	public JsonNodeFunction(final JsonNode value) {
		this.value = value;
	}

	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		return Collections.singletonList(value);
	}
}
