package net.thisptr.jackson.jq.extra.functions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction({ "strftime/1", "strftime/2" })
public class StrFTimeFunction implements Function {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("strptime", in, JsonNodeType.NUMBER);

		final List<JsonNode> out = new ArrayList<>();
		try {
			for (final JsonNode fmt : args.get(0).apply(in)) {
				if (!fmt.isTextual())
					throw JsonQueryException.format("Illegal argument type: %s", fmt.getNodeType());
				final SimpleDateFormat sdf = new SimpleDateFormat(fmt.asText());
				if (args.size() == 2) {
					for (JsonNode tz : args.get(1).apply(in)) {
						if (!tz.isTextual())
							throw JsonQueryException.format("Timezone must be a string");
						sdf.setTimeZone(TimeZone.getTimeZone(tz.asText()));
						out.add(new TextNode(sdf.format((long) in.asDouble())));
					}
				} else {
					out.add(new TextNode(sdf.format((long) in.asDouble())));
				}
			}
		} catch (Exception e) {
			throw new JsonQueryException(e);
		}
		return out;
	}
}
