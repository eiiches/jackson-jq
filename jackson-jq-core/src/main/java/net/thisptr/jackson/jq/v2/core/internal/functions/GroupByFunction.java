package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "group_by", nargs = 1)
public class GroupByFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			Preconditions.checkInputType(jsonProvider, "group_by", in, JsonNodeType.ARRAY);

			JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
			TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
			Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				JsonNode i = iter.next();
				List<JsonNode> fxList = new ArrayList<>();
				args.get(0).apply(frame, i, fxList::add);
				JsonNode fx = JsonNodeUtils.asArrayNode(jsonProvider, fxList);
				List<JsonNode> values = result.computeIfAbsent(fx, k -> new ArrayList<>());
				values.add(i);
			}

			List<JsonNode> groups = new ArrayList<>(result.size());
			for (List<JsonNode> values : result.values())
				groups.add(JsonNodeUtils.asArrayNode(jsonProvider, values));
			output.emit(JsonNodeUtils.asArrayNode(jsonProvider, groups), null);
		};
	}
}
