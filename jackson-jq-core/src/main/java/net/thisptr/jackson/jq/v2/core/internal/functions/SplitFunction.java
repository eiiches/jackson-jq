package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "split", nargs = 1)
public class SplitFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			args.get(0).apply(frame, in, (sep) -> {
				if (jsonProvider.getNodeType(in) != JsonNodeType.STRING || jsonProvider.getNodeType(sep) != JsonNodeType.STRING)
					throw new JsonQueryTypeException("split input and separator must be strings");

				JsonNode row = jsonProvider.createArray();
				for (String seg : Strings.split(jsonProvider.asText(in), jsonProvider.asText(sep)))
					jsonProvider.add(row, jsonProvider.createString(seg));

				output.emit(row, null);
			});
		};
	}
}
