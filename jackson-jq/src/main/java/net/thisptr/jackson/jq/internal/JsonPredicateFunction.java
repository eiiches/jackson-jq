package net.thisptr.jackson.jq.internal;

import java.util.List;
import java.util.function.Predicate;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class JsonPredicateFunction<JsonNode> implements Function<JsonNode> {
	private Predicate<JsonNode> predicate;

	public JsonPredicateFunction(final Predicate<JsonNode> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createBoolean(predicate.test(in)), null);
	}
}