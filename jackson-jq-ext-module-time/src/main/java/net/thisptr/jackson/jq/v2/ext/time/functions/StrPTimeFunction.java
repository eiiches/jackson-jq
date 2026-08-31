package net.thisptr.jackson.jq.v2.ext.time.functions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.ext.time.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class StrPTimeFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				@Var boolean allOne = true;
				for (Expression<Context, JsonNode> arg : args) {
					Cardinality c = arg.getCardinality();
					if (c == Cardinality.ZERO)
						return Cardinality.ZERO;
					if (c != Cardinality.ONE)
						allOne = false;
				}
				return allOne ? Cardinality.ONE : Cardinality.UNKNOWN;
			}

			@Override
			public boolean dependsOnExternalState() {
				return args.size() == 1 || args.stream().anyMatch(Expression::dependsOnExternalState);
			}

			@Override
			public boolean dependsOnInput() {
				return true;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				Preconditions.checkInputType(jsonProvider, "strptime", in, JsonNodeType.STRING);
				try {
					args.get(0).apply(context, in, UntrackedPath.getInstance(), (fmt, opath) -> {
						if (jsonProvider.getNodeType(fmt) != JsonNodeType.STRING)
							throw new JsonQueryException(String.format("Illegal argument type: %s", jsonProvider.getNodeType(fmt)));
						SimpleDateFormat sdf = new SimpleDateFormat(jsonProvider.getString(fmt));
						if (args.size() == 2) {
							args.get(1).apply(context, in, UntrackedPath.getInstance(), (tz, opath2) -> {
								if (jsonProvider.getNodeType(tz) != JsonNodeType.STRING)
									throw new JsonQueryException("Timezone must be a string");
								sdf.setTimeZone(TimeZone.getTimeZone(jsonProvider.getString(tz)));
								try {
									output.emit(jsonProvider.createNumber(sdf.parse(jsonProvider.getString(in)).getTime()), UntrackedPath.getInstance());
								} catch (ParseException e) {
									throw new JsonQueryException(e);
								}
							});
						} else {
							try {
								output.emit(jsonProvider.createNumber(sdf.parse(jsonProvider.getString(in)).getTime()), UntrackedPath.getInstance());
							} catch (ParseException e) {
								throw new JsonQueryException(e);
							}
						}
					});
				} catch (Exception e) {
					throw new JsonQueryException(e);
				}
			}
		};
	}
}
