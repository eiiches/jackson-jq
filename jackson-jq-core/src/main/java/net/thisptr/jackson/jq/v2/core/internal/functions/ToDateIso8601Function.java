package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.time.DateTimeException;
import java.time.Instant;
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
@FunctionRegistration(name = "todateiso8601", nargs = 0)
public class ToDateIso8601Function implements Function {

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output) -> {

				Preconditions.checkInputType(jsonProvider, "todateiso8601", in, JsonNodeType.NUMBER);
		double epochDouble = jsonProvider.asDouble(in);
		if (Double.isNaN(epochDouble))
			throw new JsonQueryException("todateiso8601 cannot be applied to nan");
		if (Double.isInfinite(epochDouble))
			throw new JsonQueryException("todateiso8601 cannot be applied to infinite");
		try {
			long epochSeconds = (long) epochDouble;
			String iso8601String = Instant.ofEpochSecond(epochSeconds).toString();
			output.emit(jsonProvider.createString(iso8601String), null);
		} catch (DateTimeException e) {
			throw new JsonQueryException(e);
		}
		};
}
}
