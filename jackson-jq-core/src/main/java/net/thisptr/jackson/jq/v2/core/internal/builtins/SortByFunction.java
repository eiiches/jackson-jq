package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "sort_by", nargs = 1)
public class SortByFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((frame, items, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "sort_by", items, JsonNodeType.ARRAY);

			JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
			List<Pair<JsonNode, JsonNode>> zipped = new ArrayList<>(jsonProvider.getArrayLength(items));
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(items);
			while (iter.hasNext()) {
				JsonNode item = iter.next();
				List<JsonNode> values = new ArrayList<>();
				args.get(0).apply(frame, item, UntrackedPath.getInstance(), (v, opath) -> values.add(v));
				zipped.add(Pair.of(item, jsonProvider.createArray(values)));
			}

			zipped.sort((o1, o2) -> comparator.compare(o1._2, o2._2));

			output.emit(JsonNodeUtils.asArrayNode(jsonProvider, Pair._1(zipped)), UntrackedPath.getInstance());
		});
	}
}
