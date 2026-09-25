package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.json.operations.BinaryOperations;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@FunctionRegistration(name = "range", nargs = 1)
@FunctionRegistration(name = "range", nargs = 2)
@FunctionRegistration(name = "range", nargs = 3)
public class RangeFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");

	/**
	 * Indexed by argument count; index 0 is unused because {@code range/0} does not exist.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(), typeSchemes(1), typeSchemes(2), typeSchemes(3));

	private static List<TypeScheme<FunctionType>> typeSchemes(int totalArguments) {
		return List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, NumericType.getInstance(), Collections.nCopies(totalArguments, FilterType.of(INPUT, NumericType.getInstance())).toArray(FilterType[]::new))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 3)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		return ExpressionPropertiesUtils.forwardAll(Cardinality.UNKNOWN, false, false, arguments);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindCtx, List<Expression<Context, JsonNode>> args) {
		JsonProvider<JsonNode> jsonProvider = bindCtx.getJsonProvider();
		Version version = bindCtx.getJqVersion();
		return (frame, in, ipath, output) -> {
			if (args.size() == 1) {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (end, opath) -> {
					range1(jsonProvider, output, end);
				});
			} else if (args.size() == 2) {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (start, opath) -> {
					if (version.compareTo(Versions.JQ_1_5) <= 0) {
						Object[] cur = new Object[] { start }; // only reset when start changes [v1.5]
						args.get(1).apply(frame, in, UntrackedPath.getInstance(), (end, opath2) -> {
							cur[0] = range2(jsonProvider, output, (JsonNode) cur[0], end);
						});
					} else {
						args.get(1).apply(frame, in, UntrackedPath.getInstance(), (end, opath2) -> {
							range2(jsonProvider, output, start, end);
						});
					}
				});
			} else {
				args.get(0).apply(frame, in, UntrackedPath.getInstance(), (start, opath) -> {
					args.get(1).apply(frame, in, UntrackedPath.getInstance(), (end, opath2) -> {
						args.get(2).apply(frame, in, UntrackedPath.getInstance(), (incr, opath3) -> {
							range3(jsonProvider, frame.getRuntimeLimits(), output, start, end, incr, version);
						});
					});
				});
			}
		};
	}

	private static <JsonNode> void range1(JsonProvider<JsonNode> jsonProvider, Output<JsonNode> output, JsonNode end) throws JsonQueryException {
		range2(jsonProvider, output, jsonProvider.createNumber(0), end);
	}

	private static <JsonNode> JsonNode range2(JsonProvider<JsonNode> jsonProvider, Output<JsonNode> output, JsonNode start, JsonNode end) throws JsonQueryException {
		if (!jsonProvider.isNumber(start) || !jsonProvider.isNumber(end))
			throw new JsonQueryTypeException("Range bounds must be numeric");
		double _start = jsonProvider.getNumberAsDoubleRounded(start);
		double _end = jsonProvider.getNumberAsDoubleRounded(end);
		@Var double i;
		for (i = _start; i < _end; i += 1)
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, i), UntrackedPath.getInstance());
		return JsonNodeUtils.asNumericNode(jsonProvider, i);
	}

	private static <JsonNode> void range3(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, Output<JsonNode> output, JsonNode start, JsonNode end, JsonNode incr, Version version) throws JsonQueryException {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		int dir = Integer.signum(comparator.compare(jsonProvider.createNumber(0), incr));
		if (dir == 0)
			return;
		@Var JsonNode cur = start;
		while (Integer.signum(comparator.compare(cur, end)) == dir) {
			output.emit(cur, UntrackedPath.getInstance());
			cur = BinaryOperations.plus(jsonProvider, limits, cur, incr, version);
		}
	}
}
