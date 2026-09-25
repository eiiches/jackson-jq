package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
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
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "sort_by", nargs = 1)
public class SortByFunction implements Function {
	private static final TypeVariable ELEMENT = TypeVariable.of("Element");
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(ELEMENT, AnyType.getInstance()), FunctionType.of(ArrayType.of(ELEMENT), ArrayType.of(ELEMENT), FilterType.of(ELEMENT, AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return TYPE_SCHEMES;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardDependencies(Cardinality.ONE, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		return (frame, items, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "sort_by", items, JsonNodeType.ARRAY);

			JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
			List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(jsonProvider.getArrayLength(items));
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(items);
			while (iter.hasNext()) {
				JsonNode item = iter.next();
				List<JsonNode> values = new ArrayList<>();
				args.get(0).apply(frame, item, UntrackedPath.getInstance(), (v, opath) -> values.add(v));
				zipped.add(Pair.of(item, jsonProvider.createArray(values)));
			}

			zipped.sort((o1, o2) -> comparator.compare(o1._2, o2._2));

			output.emit(JsonNodeUtils.asArrayNode(jsonProvider, Pair._1(zipped)), UntrackedPath.getInstance());
		};
	}
}
