package net.thisptr.jackson.jq.v2.ext.time.functions;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import net.thisptr.jackson.jq.v2.ext.time.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class StrFTimeFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "strftime", in, JsonNodeType.NUMBER);

			try {
				args.get(0).apply(frame, in, (fmt) -> {
					if (jsonProvider.getNodeType(fmt) != JsonNodeType.STRING)
						throw new JsonQueryException(String.format("Illegal argument type: %s", jsonProvider.getNodeType(fmt)));
					SimpleDateFormat sdf = new SimpleDateFormat(jsonProvider.asText(fmt));
					if (args.size() == 2) {
						args.get(1).apply(frame, in, (tz) -> {
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
		};
	}
}
