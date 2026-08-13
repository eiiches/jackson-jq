package net.thisptr.jackson.jq.v2.core.internal;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class JsonPredicateFunction<JsonNode> implements Function {
	private Predicate<JsonNode> predicate;

	public JsonPredicateFunction(Predicate<JsonNode> predicate) {
		this.predicate = predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <InputNode> void apply(Scope<InputNode> scope, List<Expression> args, InputNode in, @Nullable Path<InputNode> ipath, PathOutput<InputNode> output, Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createBoolean(predicate.test((JsonNode) in)), null);
	}
}
