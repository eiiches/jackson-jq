package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Splits a number of seconds since the epoch into the parts of a date and a time, which is what
 * {@code gmtime} and {@code localtime} differ only in the time zone of.
 */
abstract class AbstractTimeSplitFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(NumericType.getInstance(), BrokenDownTimes.TYPE)));

	private final String name;

	private final boolean localZone;

	AbstractTimeSplitFunction(String name, boolean localZone) {
		this.name = name;
		this.localZone = localZone;
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, localZone, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (scope, in, ipath, output) -> {
			if (!jsonProvider.isNumber(in))
				throw new JsonQueryException(name + "() requires numeric inputs");
			BrokenDownTime time;
			try {
				double epochSeconds = jsonProvider.getNumberAsDoubleRounded(in);
				time = localZone
						? BrokenDownTime.ofEpochSeconds(epochSeconds, ZoneId.systemDefault())
						: BrokenDownTime.ofEpochSecondsUtc(epochSeconds);
			} catch (DateTimeException e) {
				throw new JsonQueryException("error converting number of seconds since epoch to datetime", e);
			}
			output.emit(BrokenDownTimes.write(jsonProvider, time, ""), UntrackedPath.getInstance());
		};
	}
}
