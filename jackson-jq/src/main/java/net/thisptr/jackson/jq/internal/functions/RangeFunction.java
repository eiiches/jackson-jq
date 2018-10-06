package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.operators.PlusOperator;
import net.thisptr.jackson.jq.path.Path;

@BuiltinFunction({ "range/1", "range/2", "range/3" })
public class RangeFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (end) -> {
				range1(output, end);
			});
		} else if (args.size() == 2) {
			args.get(0).apply(scope, in, (start) -> {
				if (version.compareTo(Versions.JQ_1_5) <= 0) {
					final JsonNode[] cur = new JsonNode[] { start }; // only reset when start changes [v1.5]
					args.get(1).apply(scope, in, (end) -> {
						cur[0] = range2(output, cur[0], end);
					});
				} else {
					args.get(1).apply(scope, in, (end) -> {
						range2(output, start, end);
					});
				}
			});
		} else {
			args.get(0).apply(scope, in, (start) -> {
				args.get(1).apply(scope, in, (end) -> {
					args.get(2).apply(scope, in, (incr) -> {
						range3(output, start, end, incr);
					});
				});
			});
		}
	}

	private static void range1(final PathOutput output, final JsonNode end) throws JsonQueryException {
		range2(output, IntNode.valueOf(0), end);
	}

	private static JsonNode range2(final PathOutput output, final JsonNode start, final JsonNode end) throws JsonQueryException {
		if (!start.isNumber() || !end.isNumber())
			throw new JsonQueryTypeException("Range bounds must be numeric");
		final double _start = start.asDouble();
		final double _end = end.asDouble();
		double i;
		for (i = _start; i < _end; i += 1)
			output.emit(JsonNodeUtils.asNumericNode(i), null);
		return JsonNodeUtils.asNumericNode(i);
	}

	private static final JsonNodeComparator COMPARATOR = JsonNodeComparator.getInstance();
	private static final PlusOperator OPERATOR = new PlusOperator();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static void range3(final PathOutput output, final JsonNode start, final JsonNode end, final JsonNode incr) throws JsonQueryException {
		final int dir = Integer.signum(COMPARATOR.compare(IntNode.valueOf(0), incr));
		if (dir == 0)
			return;
		JsonNode cur = start;
		while (Integer.signum(COMPARATOR.compare(cur, end)) == dir) {
			output.emit(cur, null);
			cur = OPERATOR.apply(MAPPER, cur, incr);
		}
	}
}
