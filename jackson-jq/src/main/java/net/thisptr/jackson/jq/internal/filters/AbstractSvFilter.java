package net.thisptr.jackson.jq.internal.filters;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;

public abstract class AbstractSvFilter implements Function {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		if (!in.isArray())
			throw new JsonQueryTypeException("%s cannot be %s-formatted, only array", in, name());

		boolean heading = true;
		final StringBuilder row = new StringBuilder();
		for (final JsonNode col : in) {
			if (!heading)
				appendSeparator(row);

			if (col.isTextual()) {
				appendEscaped(row, col.asText());
			} else if (col.isNull()) {
				// empty
			} else if (col.isBoolean() || col.isNumber()) {
				try {
					row.append(scope.getObjectMapper().writeValueAsString(col));
				} catch (JsonProcessingException e) {
					throw new JsonQueryException(e);
				}
			} else {
				throw new JsonQueryTypeException("%s is not valid in a csv row", col);
			}

			heading = false;
		}

		output.emit(TextNode.valueOf(row.toString()));
	}
}