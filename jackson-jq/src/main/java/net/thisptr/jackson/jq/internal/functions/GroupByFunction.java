package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
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
public class GroupByFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "group_by", in, JsonNodeType.ARRAY);

		final JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		final TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			final JsonNode i = iter.next();
			final JsonNode fx = JsonQueryUtils.applyToArrayNode(args.get(0), scope, i);
			List<JsonNode> values = result.computeIfAbsent(fx, k -> new ArrayList<>());
			values.add(i);
		}

		final List<JsonNode> groups = new ArrayList<>(result.size());
		for (final List<JsonNode> values : result.values())
			groups.add(JsonNodeUtils.asArrayNode(jsonProvider, values));
		output.emit(JsonNodeUtils.asArrayNode(jsonProvider, groups), null);
	}
}
