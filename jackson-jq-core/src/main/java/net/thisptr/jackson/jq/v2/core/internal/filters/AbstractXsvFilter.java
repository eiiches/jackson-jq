package net.thisptr.jackson.jq.v2.core.internal.filters;

import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;

public abstract class AbstractXsvFilter implements Function {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY)
				throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be %s-formatted, only array", in, name());

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
				} else if (colType == JsonNodeType.NULL || (colType == JsonNodeType.NUMBER && Double.isNaN(jsonProvider.asDouble(col)))) {
					// empty
				} else if (colType == JsonNodeType.BOOLEAN || colType == JsonNodeType.NUMBER) {
					row.append(JsonNodeUtils.toString(jsonProvider, col, version));
				} else {
					throw new JsonQueryTypeException(jsonProvider, version, "%s is not valid in a csv row", col);
				}

				heading = false;
			}

			output.emit(jsonProvider.createString(row.toString()), null);
		});
	}
}
