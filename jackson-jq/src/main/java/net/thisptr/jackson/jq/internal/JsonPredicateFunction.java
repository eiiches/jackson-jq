package net.thisptr.jackson.jq.internal;

import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JsonPredicateFunction implements Function {
	private Predicate<JsonNode> predicate;

	public JsonPredicateFunction(final Predicate<JsonNode> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		output.emit(BooleanNode.valueOf(predicate.test(in)));
	}
}