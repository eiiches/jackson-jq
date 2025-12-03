package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractAtFormattingFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		final String text;
		try {
			text = in.isString() ? in.asString() : scope.getObjectMapper().writeValueAsString(in);
		} catch (JacksonException e) {
			throw new JsonQueryException(e);
		}
		output.emit(new StringNode(convert(text)), null);
	}

	public abstract String convert(String text) throws JsonQueryException;
}
