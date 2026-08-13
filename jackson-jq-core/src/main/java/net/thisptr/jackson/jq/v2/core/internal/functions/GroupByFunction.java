package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonQueryUtils;
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
@FunctionRegistration("group_by/1")
public class GroupByFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "group_by", in, JsonNodeType.ARRAY);

		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		TreeMap<JsonNode, List<JsonNode>> result = new TreeMap<>(comparator);
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			JsonNode i = iter.next();
			JsonNode fx = JsonQueryUtils.applyToArrayNode(args.get(0), scope, i);
			List<JsonNode> values = result.computeIfAbsent(fx, k -> new ArrayList<>());
			values.add(i);
		}

		List<JsonNode> groups = new ArrayList<>(result.size());
		for (List<JsonNode> values : result.values())
			groups.add(JsonNodeUtils.asArrayNode(jsonProvider, values));
		output.emit(JsonNodeUtils.asArrayNode(jsonProvider, groups), null);
	}
}
