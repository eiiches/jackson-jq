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
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "type", nargs = 0)
public class TypeFunction implements Function {
	private static final Type ALL_TYPES = UnionType.of(
			StringType.of("null"),
			StringType.of("boolean"),
			StringType.of("number"),
			StringType.of("string"),
			StringType.of("binary"),
			StringType.of("array"),
			StringType.of("object"));

	// An overload per kind of value, naming the very string that kind answers. That is all the narrowing
	// of a type test needs: `if type == "number"` compares two known strings once the input is one kind,
	// and a kind whose comparison cannot come out true is left to the other branch.
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(NullType.getInstance(), StringType.of("null"))),
			TypeScheme.of(FunctionType.of(BooleanType.getInstance(), StringType.of("boolean"))),
			TypeScheme.of(FunctionType.of(NumericType.getInstance(), StringType.of("number"))),
			TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.of("string"))),
			// A provider with binary nodes answers "binary"; one without represents binary data as a base64-encoded string.
			TypeScheme.of(FunctionType.of(BinaryType.getInstance(), UnionType.of(StringType.of("binary"), StringType.of("string")))),
			TypeScheme.of(FunctionType.of(ArrayType.of(AnyType.getInstance()), StringType.of("array"))),
			TypeScheme.of(FunctionType.of(ObjectType.of(AnyType.getInstance()), StringType.of("object"))),
			TypeScheme.of(FunctionType.of(AnyType.getInstance(), ALL_TYPES))
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
		return (scope, in, ipath, output) -> {

			output.emit(jsonProvider.createString(JsonNodeUtils.typeOf(jsonProvider, in)), UntrackedPath.getInstance());
		};
	}
}
