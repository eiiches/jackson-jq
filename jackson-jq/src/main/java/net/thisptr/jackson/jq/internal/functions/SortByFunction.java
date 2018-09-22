package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("sort_by/1")
public class SortByFunction implements Function {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode items, final Output output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("sort_by", items, JsonNodeType.ARRAY);

		final List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(items.size());
		for (final JsonNode item : items) {
			final ArrayNode value = scope.getObjectMapper().createArrayNode();
			args.get(0).apply(scope, item, value::add);
			zipped.add(Pair.of(item, value));
		}

		zipped.sort((o1, o2) -> comparator.compare(o1._2, o2._2));

		output.emit(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), Pair._1(zipped)));
	}
}