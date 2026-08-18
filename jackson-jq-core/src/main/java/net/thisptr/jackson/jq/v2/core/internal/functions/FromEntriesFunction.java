package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "from_entries", nargs = 0)
public class FromEntriesFunction implements FunctionFactory {
	@Override
	public <JsonNode> Expression<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output, ignoredRequirePath) -> {

				JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
			throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", in);

		JsonNode out = jsonProvider.createObject();
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			JsonNode entry = iter.next();
			if (jsonProvider.getNodeType(entry) != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string \"key\"", jsonProvider.getNodeType(entry));

			@Var JsonNode key = jsonProvider.get(entry, "key");
			if (key == null)
				key = jsonProvider.get(entry, "Key");
			if (key == null)
				key = jsonProvider.get(entry, "name");
			if (key == null)
				key = jsonProvider.get(entry, "Name");
			if (key == null || jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot use %s as object key", key == null ? jsonProvider.createNull() : key);

			@Var JsonNode value = jsonProvider.get(entry, "value");
			if (value == null)
				value = jsonProvider.get(entry, "Value");

			jsonProvider.set(out, jsonProvider.asText(key), value == null ? jsonProvider.createNull() : value);
		}

		output.emit(out, null);
		};
}
}
