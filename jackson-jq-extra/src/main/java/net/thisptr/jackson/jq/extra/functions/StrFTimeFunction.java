package net.thisptr.jackson.jq.extra.functions;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction({ "strftime/1", "strftime/2" })
public class StrFTimeFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("strptime", in, JsonNodeType.NUMBER);

		try {
			args.get(0).apply(scope, in, (fmt) -> {
				if (!fmt.isTextual())
					throw new JsonQueryTypeException("Illegal argument type: %s", fmt.getNodeType());
				final SimpleDateFormat sdf = new SimpleDateFormat(fmt.asText());
				if (args.size() == 2) {
					args.get(1).apply(scope, in, (tz) -> {
						if (!tz.isTextual())
							throw new JsonQueryTypeException("Timezone must be a string");
						sdf.setTimeZone(TimeZone.getTimeZone(tz.asText()));
						output.emit(new TextNode(sdf.format((long) in.asDouble())));
					});
				} else {
					output.emit(new TextNode(sdf.format((long) in.asDouble())));
				}
			});
		} catch (Exception e) {
			throw new JsonQueryException(e);
		}
	}
}
