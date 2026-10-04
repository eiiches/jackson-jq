package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractTrimStrFunction implements Function {
	private static final TypeVariable T = TypeVariable.of("T");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), StringType.getInstance(), FilterType.of(StringType.getInstance(), AnyType.getInstance()))),
			identity(NullType.getInstance()),
			identity(BooleanType.getInstance()),
			identity(NumericType.getInstance()),
			identity(BinaryType.getInstance()),
			identity(ArrayType.of(AnyType.getInstance())),
			identity(ObjectType.of(AnyType.getInstance()))
	);

	private static TypeScheme<FunctionType> identity(Type bound) {
		return TypeScheme.of(Map.of(T, bound), FunctionType.of(T, T, FilterType.of(T, AnyType.getInstance())));
	}

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
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (trimText, opath) -> {
				if (!jsonProvider.isString(in) || !jsonProvider.isString(trimText)) {
					output.emit(in, ipath);
					return;
				}
				JsonNode out = jsonProvider.createString(doTrim(jsonProvider.getString(in), jsonProvider.getString(trimText)));
				output.emit(out, UntrackedPath.getInstance());
			});
		};
	}

	protected abstract String doTrim(String text, String trim);
}
