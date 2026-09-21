package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
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
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractMaxByFunction implements Function {
	private static final TypeVariable ELEMENT = TypeVariable.of("Element");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(ELEMENT, AnyType.getInstance()), FunctionType.of(ArrayType.of(ELEMENT), UnionType.of(ELEMENT, NullType.getInstance()), FilterType.of(ELEMENT, AnyType.getInstance())))
	);

	private final String fname;

	public AbstractMaxByFunction(String fname) {
		this.fname = fname;
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
			Preconditions.checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);

			@Var JsonNode maxItem = jsonProvider.createNull();
			@Var JsonNode maxValue = jsonProvider.createNull();
			@Var boolean seen = false;
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
			while (iter.hasNext()) {
				JsonNode i = iter.next();
				List<JsonNode> valueList = new ArrayList<>();
				args.get(0).apply(frame, i, UntrackedPath.getInstance(), (v, opath) -> valueList.add(v));
				JsonNode value = JsonNodeUtils.asArrayNode(jsonProvider, valueList);
				if (!seen || !isLarger(jsonProvider, maxValue, value)) {
					maxValue = value;
					maxItem = i;
					seen = true;
				}
			}

			output.emit(maxItem, UntrackedPath.getInstance());
		};
	}

	protected abstract <JsonNode> boolean isLarger(JsonProvider<JsonNode> jsonProvider, JsonNode criteria, JsonNode value);
}
