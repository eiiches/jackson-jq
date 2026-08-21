package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "to_entries", nargs = 0)
public class ToEntriesFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output) -> {

				JsonNode out = jsonProvider.createArray();
		JsonNodeType inType = jsonProvider.getNodeType(in);

		if (inType == JsonNodeType.OBJECT) {
			Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				JsonNode entryNode = jsonProvider.createObject();
				jsonProvider.set(entryNode, "key", jsonProvider.createString(entry.getKey()));
				jsonProvider.set(entryNode, "value", entry.getValue());
				jsonProvider.add(out, entryNode);
			}
		} else if (inType == JsonNodeType.ARRAY) {
			Iterator<JsonNode> iter = jsonProvider.elements(in);
			for (int i = 0; iter.hasNext(); ++i) {
				JsonNode value = iter.next();
				JsonNode entryNode = jsonProvider.createObject();
				jsonProvider.set(entryNode, "key", jsonProvider.createNumber(i));
				jsonProvider.set(entryNode, "value", value);
				jsonProvider.add(out, entryNode);
			}
		} else {
			throw new JsonQueryTypeException(jsonProvider, version, "%s has no keys", in);
		}

		output.emit(out, null);
		};
}
}
