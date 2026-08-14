package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration({ "range/1", "range/2", "range/3" })
public class RangeFunction implements Function {

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (end) -> {
				range1(jsonProvider, output, end);
			});
		} else if (args.size() == 2) {
			args.get(0).apply(scope, in, (start) -> {
				if (version.compareTo(Versions.JQ_1_5) <= 0) {
					@SuppressWarnings("unchecked")
					Object[] cur = new Object[] { start }; // only reset when start changes [v1.5]
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

	private static <JsonNode> void range1(JsonProvider<JsonNode> jsonProvider, PathOutput<JsonNode> output, JsonNode end) throws JsonQueryException {
		range2(jsonProvider, output, jsonProvider.createNumber(0), end);
	}

	private static <JsonNode> JsonNode range2(JsonProvider<JsonNode> jsonProvider, PathOutput<JsonNode> output, JsonNode start, JsonNode end) throws JsonQueryException {
		if (jsonProvider.getNodeType(start) != JsonNodeType.NUMBER || jsonProvider.getNodeType(end) != JsonNodeType.NUMBER)
			throw new JsonQueryTypeException("Range bounds must be numeric");
		double _start = jsonProvider.asDouble(start);
		double _end = jsonProvider.asDouble(end);
		@Var double i;
		for (i = _start; i < _end; i += 1)
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, i), null);
		return JsonNodeUtils.asNumericNode(jsonProvider, i);
	}

	private static <JsonNode> void range3(JsonProvider<JsonNode> jsonProvider, PathOutput<JsonNode> output, JsonNode start, JsonNode end, JsonNode incr) throws JsonQueryException {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		PlusOperator<JsonNode> operator = new PlusOperator<>();
		int dir = Integer.signum(comparator.compare(jsonProvider.createNumber(0), incr));
		if (dir == 0)
			return;
		@Var JsonNode cur = start;
		while (Integer.signum(comparator.compare(cur, end)) == dir) {
			output.emit(cur, null);
			cur = operator.apply(jsonProvider, cur, incr);
		}
	}
}
