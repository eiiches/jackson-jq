package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.JsonQueryUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("group_by/1")
public class GroupByFunction implements Function {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("group_by", in, JsonNodeType.ARRAY);

		final TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
		for (final JsonNode i : in) {
			final JsonNode fx = JsonQueryUtils.applyToArrayNode(args.get(0), scope, i);
			List<JsonNode> values = result.computeIfAbsent(fx, k -> new ArrayList<>());
			values.add(i);
		}

		final List<JsonNode> groups = new ArrayList<>(result.size());
		for (final List<JsonNode> values : result.values())
			groups.add(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), values));
		output.emit(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), groups), null);
	}
}
