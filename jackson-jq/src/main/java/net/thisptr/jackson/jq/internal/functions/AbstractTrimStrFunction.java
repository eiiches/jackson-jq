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

public abstract class AbstractTrimStrFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (trimText) -> {
			if (jsonProvider.getNodeType(in) != JsonNodeType.STRING || jsonProvider.getNodeType(trimText) != JsonNodeType.STRING) {
				output.emit(in, ipath);
				return;
			}
			final JsonNode out = jsonProvider.createString(doTrim(jsonProvider.asText(in), jsonProvider.asText(trimText)));
			output.emit(out, null);
		});
	}

	protected abstract String doTrim(final String text, final String trim);
}
