package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("to_entries/0")
public class ToEntriesFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		final ArrayNode out = scope.getObjectMapper().createArrayNode();

		if (in.isObject()) {
			for (final Entry<String, JsonNode> entry : in.properties()) {
				final ObjectNode entryNode = scope.getObjectMapper().createObjectNode();
				entryNode.set("key", new StringNode(entry.getKey()));
				entryNode.set("value", entry.getValue());
				out.add(entryNode);
			}
		} else if (in.isArray()) {
			final Iterator<JsonNode> iter = in.iterator();
			for (int i = 0; iter.hasNext(); ++i) {
				final JsonNode value = iter.next();
				final ObjectNode entryNode = scope.getObjectMapper().createObjectNode();
				entryNode.set("key", IntNode.valueOf(i));
				entryNode.set("value", value);
				out.add(entryNode);
			}
		} else {
			throw new JsonQueryTypeException("%s has no keys", in);
		}

		output.emit(out, null);
	}
}
