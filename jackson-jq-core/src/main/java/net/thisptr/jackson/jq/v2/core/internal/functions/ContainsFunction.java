package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "contains", nargs = 1)
public class ContainsFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, null, (value, opath) -> {
				if (jsonProvider.getNodeType(in) != jsonProvider.getNodeType(value)
						|| (jsonProvider.getNodeType(in) == JsonNodeType.BOOLEAN && jsonProvider.asBoolean(in) != jsonProvider.asBoolean(value))) {
					throw new JsonQueryTypeException(jsonProvider, version, "%s and %s cannot have their containment checked", in, value);
				}
				output.emit(jsonProvider.createBoolean(contains(jsonProvider, value, in)), null);
			});
		});
	}

	private static <JsonNode> boolean contains(JsonProvider<JsonNode> jsonProvider, JsonNode needle, JsonNode haystack) {
		JsonNodeType hType = jsonProvider.getNodeType(haystack);
		JsonNodeType nType = jsonProvider.getNodeType(needle);
		if (hType == JsonNodeType.STRING && nType == JsonNodeType.STRING) {
			return jsonProvider.asText(haystack).contains(jsonProvider.asText(needle));
		} else if (hType == JsonNodeType.ARRAY && nType == JsonNodeType.ARRAY) {
			Iterator<JsonNode> nIter = jsonProvider.elements(needle);
			while (nIter.hasNext()) {
				JsonNode n = nIter.next();
				@Var boolean found = false;
				Iterator<JsonNode> hIter = jsonProvider.elements(haystack);
				while (hIter.hasNext()) {
					JsonNode h = hIter.next();
					if (contains(jsonProvider, n, h)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		} else if (hType == JsonNodeType.OBJECT && nType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(needle);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> field = iter.next();
				JsonNode tmp = jsonProvider.get(haystack, field.getKey());
				if (tmp == null)
					return false;
				if (!contains(jsonProvider, field.getValue(), tmp))
					return false;
			}
			return true;
		} else {
			return new JsonNodeComparator<>(jsonProvider).compare(haystack, needle) == 0;
		}
	}
}
