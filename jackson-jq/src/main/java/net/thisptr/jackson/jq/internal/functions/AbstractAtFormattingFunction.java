package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public abstract class AbstractAtFormattingFunction implements Function {

	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		final String text = in.isTextual() ? in.asText() : in.toString();
		return Collections.<JsonNode> singletonList(new TextNode(convert(text)));
	}

	public abstract String convert(String text) throws JsonQueryException;
}
