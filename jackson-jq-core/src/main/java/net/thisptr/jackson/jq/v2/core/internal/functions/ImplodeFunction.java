package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("implode/0")
public class ImplodeFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputArrayType(jsonProvider, "implode", in, JsonNodeType.NUMBER);

		StringBuilder builder = new StringBuilder();
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			JsonNode ch = iter.next();
			int intVal = jsonProvider.asInt(ch);
			double doubleVal = jsonProvider.asDouble(ch);
			if (intVal == doubleVal) {
				builder.append((char) intVal);
			} else {
				throw new JsonQueryException("input to implode() must be a list of codepoints; " + jsonProvider.getNodeType(ch) + " found");
			}
		}

		output.emit(jsonProvider.createString(builder.toString()), null);
	}
}
