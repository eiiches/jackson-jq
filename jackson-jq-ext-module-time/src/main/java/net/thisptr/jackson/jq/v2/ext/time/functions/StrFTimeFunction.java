package net.thisptr.jackson.jq.v2.ext.time.functions;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import net.thisptr.jackson.jq.v2.ext.time.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class StrFTimeFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "strftime", in, JsonNodeType.NUMBER);

		try {
			args.get(0).apply(scope, in, (fmt) -> {
				if (jsonProvider.getNodeType(fmt) != JsonNodeType.STRING)
					throw new JsonQueryException(String.format("Illegal argument type: %s", jsonProvider.getNodeType(fmt)));
				SimpleDateFormat sdf = new SimpleDateFormat(jsonProvider.asText(fmt));
				if (args.size() == 2) {
					args.get(1).apply(scope, in, (tz) -> {
						if (jsonProvider.getNodeType(tz) != JsonNodeType.STRING)
							throw new JsonQueryException("Timezone must be a string");
						sdf.setTimeZone(TimeZone.getTimeZone(jsonProvider.asText(tz)));
						output.emit(jsonProvider.createString(sdf.format((long) jsonProvider.asDouble(in))), null);
					});
				} else {
					output.emit(jsonProvider.createString(sdf.format((long) jsonProvider.asDouble(in))), null);
				}
			});
		} catch (Exception e) {
			throw new JsonQueryException(e);
		}
	}
}
