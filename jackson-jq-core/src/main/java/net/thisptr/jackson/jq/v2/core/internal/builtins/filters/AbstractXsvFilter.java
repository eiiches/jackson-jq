package net.thisptr.jackson.jq.v2.core.internal.builtins.filters;

import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractXsvFilter implements Function {
	private static final Type SCALAR = UnionType.of(StringType.getInstance(), NumericType.getInstance(), BooleanType.getInstance(), NullType.getInstance());
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(ArrayType.of(SCALAR), StringType.getInstance())));

	protected abstract String name();

	protected abstract void appendSeparator(StringBuilder builder);

	protected abstract void appendEscaped(StringBuilder builder, String text);

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
			if (!jsonProvider.isArray(in))
				throw new JsonQueryTypeException("%s cannot be %s-formatted, only array", ExceptionMessages.describe(jsonProvider, version, in), name());

			@Var boolean heading = true;
			StringBuilder row = new StringBuilder();
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
			while (iter.hasNext()) {
				JsonNode col = iter.next();
				if (!heading)
					appendSeparator(row);

				JsonNodeType colType = jsonProvider.getNodeType(col);
				if (colType == JsonNodeType.STRING) {
					appendEscaped(row, jsonProvider.getString(col));
				} else if (colType == JsonNodeType.NULL || (colType == JsonNodeType.NUMBER && Double.isNaN(jsonProvider.getNumberAsDoubleRounded(col)))) {
					// empty
				} else if (colType == JsonNodeType.BOOLEAN || colType == JsonNodeType.NUMBER) {
					row.append(JsonNodeUtils.toString(jsonProvider, col, version));
				} else {
					throw new JsonQueryTypeException("%s is not valid in a csv row", ExceptionMessages.describe(jsonProvider, version, col));
				}

				heading = false;
			}

			RuntimeLimitChecks.checkStringLength(scope.getRuntimeLimits(), row.length());
			output.emit(jsonProvider.createString(row.toString()), UntrackedPath.getInstance());
		};
	}
}
