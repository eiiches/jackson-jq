package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
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
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "index", nargs = 1)
public class IndexFunction implements Function {
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(FunctionType.of(StringType.getInstance(), UnionType.of(NumericType.getInstance(), NullType.getInstance()), FilterType.of(StringType.getInstance(), StringType.getInstance()))),
			TypeScheme.of(FunctionType.of(ArrayType.of(AnyType.getInstance()), UnionType.of(NumericType.getInstance(), NullType.getInstance()), FilterType.of(ArrayType.of(AnyType.getInstance()), AnyType.getInstance()))),
			TypeScheme.of(FunctionType.of(NullType.getInstance(), NullType.getInstance(), FilterType.of(NullType.getInstance(), AnyType.getInstance()))));

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
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (needle, opath) -> {
				IndicesFunction.emitIndices(jsonProvider, needle, in, version,
						(indices, path) -> PathOperations.resolveArrayIndex(jsonProvider, indices, UntrackedPath.getInstance(), output, 0, false, version));
			});
		};
	}
}
