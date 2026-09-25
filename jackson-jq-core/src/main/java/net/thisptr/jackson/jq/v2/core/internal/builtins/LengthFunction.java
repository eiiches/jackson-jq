package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.UnicodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
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
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "length", nargs = 0)
public class LengthFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(UnionType.of(StringType.getInstance(), ArrayType.of(AnyType.getInstance()), ObjectType.of(AnyType.getInstance()), NullType.getInstance(), NumericType.getInstance()), NumericType.getInstance())));

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
			output.emit(length(jsonProvider, in, version), UntrackedPath.getInstance());
		};
	}

	private <JsonNode> JsonNode length(JsonProvider<JsonNode> jsonProvider, JsonNode in, Version version) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.STRING) {
			return jsonProvider.createNumber(UnicodeUtils.lengthUtf32(jsonProvider.getString(in)));
		} else if (type == JsonNodeType.ARRAY) {
			return jsonProvider.createNumber(jsonProvider.getArrayLength(in));
		} else if (type == JsonNodeType.OBJECT) {
			return jsonProvider.createNumber(jsonProvider.getObjectMemberCount(in));
		} else if (type == JsonNodeType.NULL) {
			return jsonProvider.createNumber(0);
		} else if (type == JsonNodeType.NUMBER) {
			return JsonNodeUtils.asNumericNode(jsonProvider, Math.abs(jsonProvider.getNumberAsDoubleRounded(in)));
		} else {
			throw new JsonQueryTypeException("%s has no length", ExceptionMessages.describe(jsonProvider, version, in));
		}
	}
}
