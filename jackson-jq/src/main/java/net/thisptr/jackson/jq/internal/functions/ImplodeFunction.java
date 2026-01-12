package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("implode/0")
public class ImplodeFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputArrayType(jsonProvider, "implode", in, JsonNodeType.NUMBER);

		final StringBuilder builder = new StringBuilder();
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			final JsonNode ch = iter.next();
			final int intVal = jsonProvider.asInt(ch);
			final double doubleVal = jsonProvider.asDouble(ch);
			if (intVal == doubleVal) {
				builder.append((char) intVal);
			} else {
				throw new JsonQueryException("input to implode() must be a list of codepoints; " + jsonProvider.getNodeType(ch) + " found");
			}
		}

		output.emit(jsonProvider.createString(builder.toString()), null);
	}
}
