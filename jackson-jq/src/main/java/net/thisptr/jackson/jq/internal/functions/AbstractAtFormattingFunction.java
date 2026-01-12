package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractAtFormattingFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final String text = jsonProvider.getNodeType(in) == JsonNodeType.STRING
				? jsonProvider.asText(in)
				: jsonProvider.toString(in);
		output.emit(jsonProvider.createString(convert(text)), null);
	}

	public abstract String convert(String text) throws JsonQueryException;
}
