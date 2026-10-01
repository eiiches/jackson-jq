package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;
import java.util.function.UnaryOperator;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractAtFormattingFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(AnyType.getInstance(), StringType.getInstance()))
	);

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
		// jq converts the input to a string before a format can fail, and names that string in the
		// error, so [1,2] | @base64d reports a string and not an array. Built once per bind, so the
		// formats that cannot fail pay nothing for an argument they ignore.
		UnaryOperator<String> describe = text -> ExceptionMessages.describe(jsonProvider, version, jsonProvider.createString(text));
		return (scope, in, ipath, output) -> {
			String text = jsonProvider.isString(in)
					? jsonProvider.getString(in)
					: JsonNodeUtils.toString(jsonProvider, in, version);
			String formatted = convert(text, version, describe);
			RuntimeLimitChecks.checkStringLength(scope.getRuntimeLimits(), formatted.length());
			output.emit(jsonProvider.createString(formatted), UntrackedPath.getInstance());
		};
	}

	/**
	 * Formats a string the way the {@code @}-format this function implements does.
	 *
	 * @param text the input, already converted to a string the way jq converts it
	 * @param version the jq compatibility version
	 * @param describe renders its argument as {@code string (<truncated json>)}, for naming the
	 * offending value in an error the way jq names it
	 * @return the formatted text
	 * @throws JsonQueryException if {@code text} is not something this format accepts
	 */
	public abstract String convert(String text, Version version, UnaryOperator<String> describe) throws JsonQueryException;
}
