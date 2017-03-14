package net.thisptr.jackson.jq.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JsonPredicateFunction implements Function {
	private Predicate<JsonNode> predicate;

	public JsonPredicateFunction(final Predicate<JsonNode> predicate) {
		this.predicate = predicate;
	}

	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		return Collections.singletonList(BooleanNode.valueOf(predicate.test(in)));
	}
}