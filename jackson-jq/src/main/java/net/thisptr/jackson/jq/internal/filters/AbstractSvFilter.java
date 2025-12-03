package net.thisptr.jackson.jq.internal.filters;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractSvFilter implements Function {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (!in.isArray())
			throw new JsonQueryTypeException("%s cannot be %s-formatted, only array", in, name());

		boolean heading = true;
		final StringBuilder row = new StringBuilder();
		for (final JsonNode col : in) {
			if (!heading)
				appendSeparator(row);

			if (col.isString()) {
				appendEscaped(row, col.asString());
			} else if (col.isNull() || col.isNumber() && Double.isNaN(col.asDouble())) {
				// empty
			} else if (col.isBoolean() || col.isNumber()) {
				try {
					row.append(scope.getObjectMapper().writeValueAsString(col));
				} catch (JacksonException e) {
					throw new JsonQueryException(e);
				}
			} else {
				throw new JsonQueryTypeException("%s is not valid in a csv row", col);
			}

			heading = false;
		}

		output.emit(StringNode.valueOf(row.toString()), null);
	}
}
