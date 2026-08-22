package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Pair;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "sort_by", nargs = 1)
public class SortByFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((frame, items, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "sort_by", items, JsonNodeType.ARRAY);

			JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
			List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(jsonProvider.size(items));
			Iterator<JsonNode> iter = jsonProvider.elements(items);
			while (iter.hasNext()) {
				JsonNode item = iter.next();
				JsonNode value = jsonProvider.createArray();
				args.get(0).apply(frame, item, null, (v, opath) -> jsonProvider.add(value, v));
				zipped.add(Pair.of(item, value));
			}

			zipped.sort((o1, o2) -> comparator.compare(o1._2, o2._2));

			output.emit(JsonNodeUtils.asArrayNode(jsonProvider, Pair._1(zipped)), null);
		});
	}
}
