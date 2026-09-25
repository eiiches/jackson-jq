package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "not", nargs = 0)
public class NotFunction implements Function {
	// An overload per kind of value, since only `null` and `false` answer true. Saying which is which
	// lets a condition written as `... | not` decide a branch the same way the test it negates does.
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(NullType.getInstance(), BooleanType.of(true))),
			TypeScheme.of(FunctionType.of(BooleanType.of(false), BooleanType.of(true))),
			TypeScheme.of(FunctionType.of(BooleanType.of(true), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(NumericType.getInstance(), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(StringType.getInstance(), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(BinaryType.getInstance(), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(ArrayType.of(AnyType.getInstance()), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(ObjectType.of(AnyType.getInstance()), BooleanType.of(false))),
			TypeScheme.of(FunctionType.of(AnyType.getInstance(), BooleanType.getInstance())));

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
		return (scope, in, ipath, output) -> {

			output.emit(jsonProvider.createBoolean(!JsonNodeUtils.asBoolean(jsonProvider, in)), UntrackedPath.getInstance());
		};
	}
}
