package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("ltrimstr/1")
public class LTrimStrFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType("ltrimstr", in, JsonNodeType.STRING);

		final String text = in.asText();

		args.get(0).apply(scope, in, (prefixNode) -> {
			if (!prefixNode.isTextual())
				throw new JsonQueryException("1st argument to ltrimstr() must be string, got " + prefixNode.getNodeType());

			final String prefix = prefixNode.asText();

			if (!text.startsWith(prefix)) {
				output.emit(in);
			} else {
				output.emit(new TextNode(text.substring(prefix.length(), text.length())));
			}
		});
	}
}
