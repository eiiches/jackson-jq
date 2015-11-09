package net.thisptr.jackson.jq.extra.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("uridecode/0")
public class UriDecodeFunction implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("urldecode", in, JsonNodeType.STRING);

		try {
			return Collections.<JsonNode> singletonList(new TextNode(URLDecoder.decode(in.asText(), "UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
