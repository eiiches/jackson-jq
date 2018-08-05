package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("from_entries/0")
public class FromEntriesFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputArrayType("from_entries", in, JsonNodeType.OBJECT);

		final ObjectNode out = scope.getObjectMapper().createObjectNode();
		for (final JsonNode entry : in) {
			JsonNode key = entry.get("key");
			if (key == null)
				key = entry.get("Key");
			if (key == null)
				key = entry.get("name");
			if (key == null)
				key = entry.get("Name");
			if (key == null || !key.isTextual())
				throw new JsonQueryException("input elements to from_entries() must be object and have 'key', 'Key' or 'Name' field");
			JsonNode value = entry.get("value");
			if (value == null)
				value = entry.get("Value");
			out.set(key.asText(), value == null ? NullNode.getInstance() : value);
		}

		output.emit(out);
	}
}
