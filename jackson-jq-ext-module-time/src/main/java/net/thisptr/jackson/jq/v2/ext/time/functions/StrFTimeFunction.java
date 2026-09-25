package net.thisptr.jackson.jq.v2.ext.time.functions;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.ext.time.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class StrFTimeFunction implements Function {
	/**
	 * Indexed by argument count; index 0 is unused because {@code strftime/0} does not exist. The
	 * format and the timezone are both read as strings from the epoch-milliseconds input.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(), typeSchemes(1), typeSchemes(2));

	private static List<TypeScheme<FunctionType>> typeSchemes(int totalArguments) {
		return List.of(TypeScheme.of(FunctionType.of(NumericType.getInstance(), StringType.getInstance(), Collections.nCopies(totalArguments, FilterType.of(NumericType.getInstance(), StringType.getInstance())).toArray(FilterType[]::new))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 2)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		@Var boolean allOne = true;
		for (ExpressionProperties argument : arguments) {
			if (argument.cardinality() == Cardinality.ZERO)
				return new ExpressionProperties(Cardinality.ZERO, true, arguments.size() == 1
						|| arguments.stream().anyMatch(ExpressionProperties::dependsOnExternalState));
			if (argument.cardinality() != Cardinality.ONE)
				allOne = false;
		}
		boolean external = arguments.size() == 1 || arguments.stream().anyMatch(ExpressionProperties::dependsOnExternalState);
		return new ExpressionProperties(allOne ? Cardinality.ONE : Cardinality.UNKNOWN, true, external);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				Preconditions.checkInputType(jsonProvider, "strftime", in, JsonNodeType.NUMBER);
				Long epochSeconds = jsonProvider.getNumberAsLongTruncated(in);
				if (epochSeconds == null) // NaN, an infinity, or beyond long range.
					throw new JsonQueryException("date \"" + jsonProvider.format(in) + "\" does not fit in a number of seconds since the epoch");
				try {
					args.get(0).apply(context, in, UntrackedPath.getInstance(), (fmt, opath) -> {
						if (!jsonProvider.isString(fmt))
							throw new JsonQueryException(String.format("Illegal argument type: %s", jsonProvider.getNodeType(fmt)));
						SimpleDateFormat sdf = new SimpleDateFormat(jsonProvider.getString(fmt));
						if (args.size() == 2) {
							args.get(1).apply(context, in, UntrackedPath.getInstance(), (tz, opath2) -> {
								if (!jsonProvider.isString(tz))
									throw new JsonQueryException("Timezone must be a string");
								sdf.setTimeZone(TimeZone.getTimeZone(jsonProvider.getString(tz)));
								output.emit(jsonProvider.createString(sdf.format(epochSeconds)), UntrackedPath.getInstance());
							});
						} else {
							output.emit(jsonProvider.createString(sdf.format(epochSeconds)), UntrackedPath.getInstance());
						}
					});
				} catch (Exception e) {
					throw new JsonQueryException(e);
				}
			}
		};
	}
}
