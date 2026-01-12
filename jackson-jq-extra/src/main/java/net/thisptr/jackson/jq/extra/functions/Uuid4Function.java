package net.thisptr.jackson.jq.extra.functions;

import java.util.List;
import java.util.UUID;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@SuppressWarnings("rawtypes")
@AutoService(Function.class)
@BuiltinFunction("uuid4/0")
public class Uuid4Function<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		output.emit(scope.jsonProvider().createString(UUID.randomUUID().toString()), null);
	}
}
