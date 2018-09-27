package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("join/1")
public class JoinFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (sep) -> {
			if (!in.isArray() && !in.isObject())
				throw new JsonQueryTypeException("Cannot iterate over %s", in);

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
				} else if (version.compareTo(Versions.JQ_1_6) >= 0 && (item.isNumber() || item.isBoolean())) {
					// https://github.com/stedolan/jq/commit/e17ccf229723d776c0d49341665256b855c70bda
					// https://github.com/stedolan/jq/issues/930
					builder.append(item.toString());
				} else {
					if (version.compareTo(Versions.JQ_1_6) >= 0)
						throw new JsonQueryTypeException(new TextNode(builder.toString()), item, "cannot be added");
					throw new JsonQueryTypeException(sep, item, "cannot be added");
				}

				isep = sep;
			}
			output.emit(new TextNode(builder.toString()));
		});
	}
}
