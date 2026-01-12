package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction({ "range/1", "range/2", "range/3" })
public class RangeFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (end) -> {
				range1(jsonProvider, output, end);
			});
		} else if (args.size() == 2) {
			args.get(0).apply(scope, in, (start) -> {
				if (version.compareTo(Versions.JQ_1_5) <= 0) {
					@SuppressWarnings("unchecked")
					final Object[] cur = new Object[] { start }; // only reset when start changes [v1.5]
					args.get(1).apply(scope, in, (end) -> {
						cur[0] = range2(jsonProvider, output, (JsonNode) cur[0], end);
					});
				} else {
					args.get(1).apply(scope, in, (end) -> {
						range2(jsonProvider, output, start, end);
					});
				}
			});
		} else {
			args.get(0).apply(scope, in, (start) -> {
				args.get(1).apply(scope, in, (end) -> {
					args.get(2).apply(scope, in, (incr) -> {
						range3(jsonProvider, output, start, end, incr);
					});
				});
			});
		}
	}

	private static <JsonNode> void range1(final JsonProvider<JsonNode> jsonProvider, final PathOutput<JsonNode> output, final JsonNode end) throws JsonQueryException {
		range2(jsonProvider, output, jsonProvider.createInt(0), end);
	}

	private static <JsonNode> JsonNode range2(final JsonProvider<JsonNode> jsonProvider, final PathOutput<JsonNode> output, final JsonNode start, final JsonNode end) throws JsonQueryException {
		if (jsonProvider.getNodeType(start) != JsonNodeType.NUMBER || jsonProvider.getNodeType(end) != JsonNodeType.NUMBER)
			throw new JsonQueryTypeException("Range bounds must be numeric");
		final double _start = jsonProvider.asDouble(start);
		final double _end = jsonProvider.asDouble(end);
		double i;
		for (i = _start; i < _end; i += 1)
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, i), null);
		return JsonNodeUtils.asNumericNode(jsonProvider, i);
	}

	private static <JsonNode> void range3(final JsonProvider<JsonNode> jsonProvider, final PathOutput<JsonNode> output, final JsonNode start, final JsonNode end, final JsonNode incr) throws JsonQueryException {
		final JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		final PlusOperator<JsonNode> operator = new PlusOperator<>();
		final int dir = Integer.signum(comparator.compare(jsonProvider.createInt(0), incr));
		if (dir == 0)
			return;
		JsonNode cur = start;
		while (Integer.signum(comparator.compare(cur, end)) == dir) {
			output.emit(cur, null);
			cur = operator.apply(jsonProvider, cur, incr);
		}
	}
}
