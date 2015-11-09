package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("scan/1")
public class ScanFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("scan", in, JsonNodeType.STRING);

		final String inputText = in.asText();
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode regex : args.get(0).apply(scope, in)) {
			if (!regex.isTextual())
				throw new JsonQueryException("1st argument to scan() must be a string");

			final Pattern p = Pattern.compile(regex.asText());
			final Matcher m = p.matcher(inputText);
			while (m.find())
				out.add(new TextNode(m.group()));
		}
		return out;
	}
}
