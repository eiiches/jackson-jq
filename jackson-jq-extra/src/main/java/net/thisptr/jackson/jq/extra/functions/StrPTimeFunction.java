package net.thisptr.jackson.jq.extra.functions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

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
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@SuppressWarnings("rawtypes")
@AutoService(Function.class)
@BuiltinFunction({ "strptime/1", "strptime/2" })
public class StrPTimeFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "strptime", in, JsonNodeType.STRING);

		try {
			args.get(0).apply(scope, in, (fmt) -> {
				if (jsonProvider.getNodeType(fmt) != JsonNodeType.STRING)
					throw new JsonQueryTypeException("Illegal argument type: %s", jsonProvider.getNodeType(fmt));
				final SimpleDateFormat sdf = new SimpleDateFormat(jsonProvider.asText(fmt));
				if (args.size() == 2) {
					args.get(1).apply(scope, in, (tz) -> {
						if (jsonProvider.getNodeType(tz) != JsonNodeType.STRING)
							throw new JsonQueryTypeException("Timezone must be a string");
						sdf.setTimeZone(TimeZone.getTimeZone(jsonProvider.asText(tz)));
						try {
							output.emit(jsonProvider.createLong(sdf.parse(jsonProvider.asText(in)).getTime()), null);
						} catch (ParseException e) {
							throw new JsonQueryException(e);
						}
					});
				} else {
					try {
						output.emit(jsonProvider.createLong(sdf.parse(jsonProvider.asText(in)).getTime()), null);
					} catch (ParseException e) {
						throw new JsonQueryException(e);
					}
				}
			});
		} catch (Exception e) {
			throw new JsonQueryException(e);
		}
	}
}
