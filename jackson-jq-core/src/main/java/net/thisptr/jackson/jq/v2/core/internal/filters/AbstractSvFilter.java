package net.thisptr.jackson.jq.v2.core.internal.filters;

import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class AbstractSvFilter implements Function {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY)
			throw new JsonQueryTypeException(jsonProvider, "%s cannot be %s-formatted, only array", in, name());

		@Var boolean heading = true;
		StringBuilder row = new StringBuilder();
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			JsonNode col = iter.next();
			if (!heading)
				appendSeparator(row);

			JsonNodeType colType = jsonProvider.getNodeType(col);
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
