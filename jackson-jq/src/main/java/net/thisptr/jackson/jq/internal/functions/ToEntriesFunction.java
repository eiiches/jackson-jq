package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("to_entries/0")
public class ToEntriesFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("to_entries", in, JsonNodeType.OBJECT);

		final ArrayNode out = scope.getObjectMapper().createArrayNode();
		final Iterator<Entry<String, JsonNode>> iter = in.fields();
		while (iter.hasNext()) {
			final Entry<String, JsonNode> entry = iter.next();
			final ObjectNode entryNode = scope.getObjectMapper().createObjectNode();
			entryNode.set("key", new TextNode(entry.getKey()));
			entryNode.set("value", entry.getValue());
			out.add(entryNode);
		}

		return Collections.singletonList((JsonNode) out);
	}
}
