package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("to_entries/0")
public class ToEntriesFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
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

		output.emit(out);
	}
}
