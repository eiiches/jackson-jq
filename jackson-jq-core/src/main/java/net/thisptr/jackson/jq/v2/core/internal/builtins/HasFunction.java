package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
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
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "has", nargs = 1)
public class HasFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(ObjectType.of(AnyType.getInstance()), BooleanType.getInstance(), FilterType.of(ObjectType.of(AnyType.getInstance()), StringType.getInstance()))),
			TypeScheme.of(FunctionType.of(ArrayType.of(AnyType.getInstance()), BooleanType.getInstance(), FilterType.of(ArrayType.of(AnyType.getInstance()), NumericType.getInstance()))),
			TypeScheme.of(FunctionType.of(NullType.getInstance(), BooleanType.getInstance(), FilterType.of(NullType.getInstance(), AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.UNKNOWN, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, in, ipath, output) -> {
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType == JsonNodeType.NULL) {
				output.emit(jsonProvider.createBoolean(false), UntrackedPath.getInstance());
				return;
			}
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (keyName, opath) -> {
				JsonNodeType keyType = jsonProvider.getNodeType(keyName);
				if (inType == JsonNodeType.OBJECT) {
					if (keyType != JsonNodeType.STRING)
						throw new JsonQueryException("argument 1 of has() must be string for object input");
					output.emit(jsonProvider.createBoolean(jsonProvider.hasObjectMember(in, jsonProvider.getString(keyName))), UntrackedPath.getInstance());
				} else if (inType == JsonNodeType.ARRAY) {
					if (keyType != JsonNodeType.NUMBER)
						throw new JsonQueryException("argument 1 of has() must be int for array input");
					// NaN, the infinities and anything outside int range are all simply absent.
					Integer keyAsInt = jsonProvider.getNumberAsIntTruncated(keyName);
					boolean present = keyAsInt != null && keyAsInt >= 0 && keyAsInt < jsonProvider.getArrayLength(in);
					output.emit(jsonProvider.createBoolean(present), UntrackedPath.getInstance());
				} else {
					throw new JsonQueryException("has() is not applicable to " + inType);
				}
			});
		};
	}
}
