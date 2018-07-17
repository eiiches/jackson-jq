package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("join/1")
public class JoinFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode sep : args.get(0).apply(scope, in)) {

			JsonNode isep = null;
			final StringBuilder builder = new StringBuilder();
			for (final JsonNode item : in) {
				if (isep != null) {
					if (isep.isTextual()) {
						builder.append(isep.asText());
					} else if (isep.isNull()) {
						// append nothing
					} else {
						throw new JsonQueryTypeException(new TextNode(builder.toString()), isep, "cannot be added");
					}
				}

				if (item.isTextual()) {
					builder.append(item.asText());
				} else if (item.isNull()) {
					// append nothing
				} else if (item.isNumber() || item.isBoolean()) {
					// https://github.com/stedolan/jq/commit/e17ccf229723d776c0d49341665256b855c70bda
					// https://github.com/stedolan/jq/issues/930
					builder.append(item.toString());
				} else {
					throw new JsonQueryTypeException(new TextNode(builder.toString()), item, "cannot be added");
				}

				isep = sep;
			}
			out.add(new TextNode(builder.toString()));
		}
		return out;
	}
}
