package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "to_entries", nargs = 0)
public class ToEntriesFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		JsonNode out = jsonProvider.createArray();
		JsonNodeType inType = jsonProvider.getNodeType(in);

		if (inType == JsonNodeType.OBJECT) {
			Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				Entry<String, JsonNode> entry = iter.next();
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
			throw new JsonQueryTypeException(jsonProvider, "%s has no keys", in);
		}

		output.emit(out, null);
	}
}
