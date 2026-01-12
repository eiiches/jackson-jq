package net.thisptr.jackson.jq.internal.filters;

import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractSvFilter<JsonNode> implements Function<JsonNode> {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY)
			throw new JsonQueryTypeException(jsonProvider, "%s cannot be %s-formatted, only array", in, name());

		boolean heading = true;
		final StringBuilder row = new StringBuilder();
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			final JsonNode col = iter.next();
			if (!heading)
				appendSeparator(row);

			final JsonNodeType colType = jsonProvider.getNodeType(col);
			if (colType == JsonNodeType.STRING) {
				appendEscaped(row, jsonProvider.asText(col));
			} else if (colType == JsonNodeType.NULL || colType == JsonNodeType.NUMBER && Double.isNaN(jsonProvider.asDouble(col))) {
				// empty
			} else if (colType == JsonNodeType.BOOLEAN || colType == JsonNodeType.NUMBER) {
				row.append(jsonProvider.toString(col));
			} else {
				throw new JsonQueryTypeException(jsonProvider, "%s is not valid in a csv row", col);
			}

			heading = false;
		}

		output.emit(jsonProvider.createString(row.toString()), null);
	}
}
