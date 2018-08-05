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

@BuiltinFunction("rtrimstr/1")
public class RTrimStrFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType("rtrimstr", in, JsonNodeType.STRING);

		final String text = in.asText();

		args.get(0).apply(scope, in, (suffixNode) -> {
			if (!suffixNode.isTextual())
				throw new JsonQueryException("1st argument to rtrimstr() must be string, got " + suffixNode.getNodeType());

			final String suffix = suffixNode.asText();

			if (!text.endsWith(suffix)) {
				output.emit(in);
			} else {
				output.emit(new TextNode(text.substring(0, text.length() - suffix.length())));
			}
		});
	}
}
