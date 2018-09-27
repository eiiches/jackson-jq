package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Strings;

@BuiltinFunction("split/1")
public class SplitFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (sep) -> {
			if (!in.isTextual() || !sep.isTextual())
				throw new JsonQueryTypeException("split input and separator must be strings");

			final ArrayNode row = scope.getObjectMapper().createArrayNode();
			for (final String seg : Strings.split(in.asText(), sep.asText()))
				row.add(new TextNode(seg));

			output.emit(row);
		});
	}
}
