package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
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
@FunctionRegistration("sort_by/1")
public class SortByFunction implements Function {
	@Override
	public <JsonNode> void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode items, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
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
