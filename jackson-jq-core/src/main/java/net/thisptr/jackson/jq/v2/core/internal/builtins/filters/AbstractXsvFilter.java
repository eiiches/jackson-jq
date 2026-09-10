package net.thisptr.jackson.jq.v2.core.internal.builtins.filters;

import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractXsvFilter implements Function {

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			if (!jsonProvider.isArray(in))
				throw new JsonQueryTypeException("%s cannot be %s-formatted, only array", ExceptionMessages.describe(jsonProvider, version, in), name());

			@Var boolean heading = true;
			StringBuilder row = new StringBuilder();
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
			while (iter.hasNext()) {
				JsonNode col = iter.next();
				if (!heading)
					appendSeparator(row);

				JsonNodeType colType = jsonProvider.getNodeType(col);
				if (colType == JsonNodeType.STRING) {
					appendEscaped(row, jsonProvider.getString(col));
				} else if (colType == JsonNodeType.NULL || (colType == JsonNodeType.NUMBER && Double.isNaN(jsonProvider.getNumberAsDoubleRounded(col)))) {
					// empty
				} else if (colType == JsonNodeType.BOOLEAN || colType == JsonNodeType.NUMBER) {
					row.append(JsonNodeUtils.toString(jsonProvider, col, version));
				} else {
					throw new JsonQueryTypeException("%s is not valid in a csv row", ExceptionMessages.describe(jsonProvider, version, col));
				}

				heading = false;
			}

			output.emit(jsonProvider.createString(row.toString()), UntrackedPath.getInstance());
		});
	}
}
