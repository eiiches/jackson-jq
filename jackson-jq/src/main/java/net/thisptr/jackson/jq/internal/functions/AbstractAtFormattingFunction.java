package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public abstract class AbstractAtFormattingFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		final String text;
		try {
			text = in.isTextual() ? in.asText() : scope.getObjectMapper().writeValueAsString(in);
		} catch (JsonProcessingException e) {
			throw new JsonQueryException(e);
		}
		output.emit(new TextNode(convert(text)));
	}

	public abstract String convert(String text) throws JsonQueryException;
}
