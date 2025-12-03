package net.thisptr.jackson.jq.extra.functions;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.StringNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction({ "strftime/1", "strftime/2" })
public class StrFTimeFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("strptime", in, JsonNodeType.NUMBER);

		try {
			args.get(0).apply(scope, in, (fmt) -> {
				if (!fmt.isString())
					throw new JsonQueryTypeException("Illegal argument type: %s", fmt.getNodeType());
				final SimpleDateFormat sdf = new SimpleDateFormat(fmt.asString());
				if (args.size() == 2) {
					args.get(1).apply(scope, in, (tz) -> {
						if (!tz.isString())
							throw new JsonQueryTypeException("Timezone must be a string");
						sdf.setTimeZone(TimeZone.getTimeZone(tz.asString()));
						output.emit(new StringNode(sdf.format((long) in.asDouble())), null);
					});
				} else {
					output.emit(new StringNode(sdf.format((long) in.asDouble())), null);
				}
			});
		} catch (Exception e) {
			throw new JsonQueryException(e);
		}
	}
}
