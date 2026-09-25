package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "paths", nargs = 1)
public class PathsFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	/**
	 * Before jq 1.7.1 the filter only ever saw descendants, whose types are not known here; from
	 * 1.7.1 on it also sees the root, so the input type is a real requirement on it.
	 */
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES_DESCENDANTS_ONLY = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, BuiltinTypes.PATH, FilterType.of(AnyType.getInstance(), AnyType.getInstance()))));
	private static final List<TypeScheme<FunctionType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, BuiltinTypes.PATH, FilterType.of(INPUT, AnyType.getInstance()))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return jqVersion.compareTo(Versions.JQ_1_7_1) >= 0 ? TYPE_SCHEMES : TYPE_SCHEMES_DESCENDANTS_ONLY;
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.UNKNOWN, true, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		/*
		 * jq redefined paths/1 in 1.7.1:
		 *
		 *   [1.5, 1.7.1): def paths(f): . as $dot | paths | select(getpath($dot; .) | f);
		 *   [1.7.1, ):    def paths(f): path(.. | select(f)) | select(length > 0);
		 *
		 * The newer definition evaluates f on the root value as well. The root path is empty, so
		 * select(length > 0) still keeps it out of the output; the only visible difference is that
		 * errors and side effects raised by f on the root now happen, before any child path is
		 * emitted.
		 */
		boolean appliesToRoot = version.compareTo(Versions.JQ_1_7_1) >= 0;
		return (frame, in, ipath, output) -> {
			List<JsonNode> stack = new ArrayList<>();
			applyRecursive(frame, jsonProvider, in, output, stack, args.get(0), appliesToRoot);
		};
	}

	private static <Context extends RuntimeContext, JsonNode> void applyRecursive(Context context, JsonProvider<JsonNode> jsonProvider, JsonNode in, Output<JsonNode> output, List<JsonNode> stack, Expression<Context, JsonNode> predicate, boolean appliesToRoot) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(context, in, UntrackedPath.getInstance(), (shouldInclude, opath) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(jsonProvider, stack), UntrackedPath.getInstance());
			});
		} else if (appliesToRoot) {
			predicate.apply(context, in, UntrackedPath.getInstance(), (shouldInclude, opath) -> {
				// The root is reached at the empty path, which is never part of the output.
			});
		}

		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.ARRAY) {
			int size = jsonProvider.getArrayLength(in);
			for (int i = 0; i < size; ++i) {
				stack.add(jsonProvider.createNumber(i));
				applyRecursive(context, jsonProvider, jsonProvider.getArrayElement(in, i), output, stack, predicate, appliesToRoot);
				stack.remove(stack.size() - 1);
			}
		} else if (inType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				stack.add(jsonProvider.createString(entry.getKey()));
				applyRecursive(context, jsonProvider, entry.getValue(), output, stack, predicate, appliesToRoot);
				stack.remove(stack.size() - 1);
			}
		}
	}
}
