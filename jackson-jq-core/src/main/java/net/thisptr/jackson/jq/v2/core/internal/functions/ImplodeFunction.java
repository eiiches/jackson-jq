package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(Function.class)
@FunctionRegistration(name = "implode", nargs = 0)
public class ImplodeFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output) -> {

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
		};
}
}
