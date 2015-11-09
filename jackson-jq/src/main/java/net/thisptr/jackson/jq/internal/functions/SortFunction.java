package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("sort/1")
public class SortFunction implements Function {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode items) throws JsonQueryException {
		Preconditions.checkInputType("sort", items, JsonNodeType.ARRAY);

		final List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(items.size());
		for (final JsonNode item : items) {
			final JsonNode value = JsonNodeUtils.asArrayNode(scope.getObjectMapper(), args.get(0).apply(scope, item));
			zipped.add(Pair.of(item, value));
		}

		Collections.sort(zipped, new Comparator<Pair<JsonNode, JsonNode>>() {
			@Override
			public int compare(final Pair<JsonNode, JsonNode> o1, final Pair<JsonNode, JsonNode> o2) {
				return comparator.compare(o1._2, o2._2);
			}
		});

		return Collections.<JsonNode> singletonList(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), Pair._1(zipped)));
	}
}