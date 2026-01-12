package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
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
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("sort_by/1")
public class SortByFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode items, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "sort_by", items, JsonNodeType.ARRAY);

		final JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		final List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(jsonProvider.size(items));
		final Iterator<JsonNode> iter = jsonProvider.elements(items);
		while (iter.hasNext()) {
			final JsonNode item = iter.next();
			final JsonNode value = jsonProvider.createArray();
			args.get(0).apply(scope, item, (v) -> jsonProvider.add(value, v));
			zipped.add(Pair.of(item, value));
		}

		zipped.sort((o1, o2) -> comparator.compare(o1._2, o2._2));

		output.emit(JsonNodeUtils.asArrayNode(jsonProvider, Pair._1(zipped)), null);
	}
}