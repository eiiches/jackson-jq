package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Strings;

@BuiltinFunction("@sh/0")
public class AtShFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		if (in.isArray()) {
			final List<String> tokens = new ArrayList<>();
			for (final JsonNode i : in) {
				if (i.isTextual()) {
					tokens.add(escape(i.asText()));
				} else if (i.isValueNode()) {
					tokens.add(toString(scope, i));
				} else {
					throw new IllegalJsonInputException(i.getNodeType() + " cannot be escaped for shell");
				}
			}
			output.emit(new TextNode(Strings.join(" ", tokens)));
		} else if (in.isTextual()) {
			output.emit(new TextNode(escape(in.asText())));
		} else if (in.isValueNode()) {
			output.emit(new TextNode(toString(scope, in)));
		} else {
			throw new IllegalJsonInputException(in.getNodeType() + " cannot be escaped for shell");
		}
	}

	private static String toString(final Scope scope, final JsonNode node) throws JsonQueryException {
		try {
			return scope.getObjectMapper().writeValueAsString(node);
		} catch (final JsonProcessingException e) {
			throw new JsonQueryException(e);
		}
	}

	public String escape(final String text) {
		final StringBuilder builder = new StringBuilder("'");
		for (final char ch : text.toCharArray()) {
			switch (ch) {
				case '\'':
					builder.append("'\\''");
					break;
				default:
					builder.append(ch);
					break;
			}
		}
		builder.append("'");
		return builder.toString();
	}
}
