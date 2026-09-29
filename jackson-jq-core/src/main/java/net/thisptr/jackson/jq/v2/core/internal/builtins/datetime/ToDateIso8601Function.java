package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * {@code todateiso8601}, a time written as an ISO 8601 datetime in UTC.
 * <p>
 * It is {@code strftime("%Y-%m-%dT%H:%M:%SZ")} under another name, and takes the same two kinds of input:
 * a number of seconds since the epoch, or a broken-down time.
 */
@FunctionRegistration(name = "todateiso8601", nargs = 0)
public class ToDateIso8601Function implements Function {
	private static final String FORMAT = "%Y-%m-%dT%H:%M:%SZ";

	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(UnionType.of(NumericType.getInstance(), BrokenDownTimes.INPUT_TYPE), StringType.getInstance())));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.ONE, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return (scope, in, ipath, output) -> {
			if (!jsonProvider.isNumber(in) && !jsonProvider.isArray(in))
				throw new JsonQueryException("strftime/1 requires parsed datetime inputs");

			BrokenDownTime time;
			if (jsonProvider.isNumber(in)) {
				try {
					time = BrokenDownTime.ofEpochSecondsUtc(jsonProvider.getNumberAsDoubleRounded(in));
				} catch (DateTimeException e) {
					throw new JsonQueryException("error converting number of seconds since epoch to datetime", e);
				}
			} else {
				time = BrokenDownTimes.read(jsonProvider, in, version, "strftime/1 requires parsed datetime inputs");
				if (version.compareTo(Versions.JQ_1_8_0) >= 0) {
					try {
						time.normalize();
					} catch (DateTimeException e) {
						throw new JsonQueryException("strftime/1 requires parsed datetime inputs", e);
					}
				}
			}
			output.emit(jsonProvider.createString(CStrftime.format(FORMAT, time, ZoneOffset.UTC)), UntrackedPath.getInstance());
		};
	}
}
