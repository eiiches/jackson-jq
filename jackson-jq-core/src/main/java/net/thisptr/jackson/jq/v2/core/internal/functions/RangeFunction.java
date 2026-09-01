package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "range", nargs = 1)
@FunctionRegistration(name = "range", nargs = 2)
@FunctionRegistration(name = "range", nargs = 3)
public class RangeFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).build((frame, in, ipath, output) -> {
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
							range3(jsonProvider, output, start, end, incr);
						});
					});
				});
			}
		});
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

	private static <JsonNode> void range3(JsonProvider<JsonNode> jsonProvider, Output<JsonNode> output, JsonNode start, JsonNode end, JsonNode incr) throws JsonQueryException {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		PlusOperator<JsonNode> operator = new PlusOperator<>();
		int dir = Integer.signum(comparator.compare(jsonProvider.createNumber(0), incr));
		if (dir == 0)
			return;
		@Var JsonNode cur = start;
		while (Integer.signum(comparator.compare(cur, end)) == dir) {
			output.emit(cur, UntrackedPath.getInstance());
			cur = operator.apply(jsonProvider, cur, incr);
		}
	}
}
