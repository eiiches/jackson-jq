package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.JsonQueryUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("group/1")
public class GroupFunction implements Function {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType("group", in, JsonNodeType.ARRAY);

		final TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
		for (final JsonNode i : in) {
			final JsonNode fx = JsonQueryUtils.applyToArrayNode(args.get(0), scope, i);
			List<JsonNode> values = result.get(fx);
			if (values == null) {
				values = new ArrayList<>();
				result.put(fx, values);
			}
			values.add(i);
		}

		final List<JsonNode> groups = new ArrayList<>(result.size());
		for (final List<JsonNode> values : result.values())
			groups.add(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), values));
		output.emit(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), groups));
	}
}
