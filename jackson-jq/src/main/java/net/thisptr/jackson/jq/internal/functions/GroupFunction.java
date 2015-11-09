package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("group/1")
public class GroupFunction implements Function {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("group", in, JsonNodeType.ARRAY);

		final TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
		for (final JsonNode i : in) {
			final JsonNode fx = JsonNodeUtils.asArrayNode(scope.getObjectMapper(), args.get(0).apply(scope, i));
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
		return Collections.<JsonNode> singletonList(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), groups));
	}
}
