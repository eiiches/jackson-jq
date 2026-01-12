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

public abstract class AbstractStartsEndsWithFunction<JsonNode> implements Function<JsonNode> {
	private final String fname;

	public AbstractStartsEndsWithFunction(final String fname) {
		this.fname = fname;
	}

	protected abstract boolean doCheck(final String text, final String needle);

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (needle) -> {
			if (jsonProvider.getNodeType(needle) != JsonNodeType.STRING || jsonProvider.getNodeType(in) != JsonNodeType.STRING)
				throw new JsonQueryException(fname + "() requires string inputs");
			output.emit(jsonProvider.createBoolean(doCheck(jsonProvider.asText(in), jsonProvider.asText(needle))), null);
		});
	}
}
