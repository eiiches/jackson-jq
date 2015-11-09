package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;

@BuiltinFunction("@sh/0")
public class AtShFunction implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		if (in.isArray()) {
			final List<String> tokens = new ArrayList<>();
			for (final JsonNode i : in) {
				if (i.isTextual()) {
					tokens.add(escape(i.asText()));
				} else if (i.isValueNode()) {
					tokens.add(i.asText());
				} else {
					throw new IllegalJsonInputException(i.getNodeType() + " cannot be escaped for shell");
				}
			}
			return Collections.<JsonNode> singletonList(new TextNode(Joiner.on(" ").join(tokens)));
		} else if (in.isTextual()) {
			return Collections.<JsonNode> singletonList(new TextNode(escape(in.asText())));
		} else if (in.isValueNode()) {
			return Collections.<JsonNode> singletonList(new TextNode(in.asText()));
		} else {
			throw new IllegalJsonInputException(in.getNodeType() + " cannot be escaped for shell");
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
