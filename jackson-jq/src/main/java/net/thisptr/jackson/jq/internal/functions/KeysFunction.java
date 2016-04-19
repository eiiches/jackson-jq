package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Lists;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("keys/0")
public class KeysFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("keys", in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

		if (in.isObject()) {
			final List<String> keys = Lists.newArrayList(in.fieldNames());
			Collections.sort(keys);

			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (final String key : keys)
				result.add(new TextNode(key));
			return Collections.singletonList((JsonNode) result);
		} else if (in.isArray()) {
			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (int i = 0; i < in.size(); ++i)
				result.add(new IntNode(i));
			return Collections.singletonList((JsonNode) result);
		} else {
			throw new IllegalStateException();
		}
	}
}
