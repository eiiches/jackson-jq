package net.thisptr.jackson.jq.internal.functions;

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
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("from_entries/0")
public class FromEntriesFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
			throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", in);

		final JsonNode out = jsonProvider.createObject();
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			final JsonNode entry = iter.next();
			if (jsonProvider.getNodeType(entry) != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with string \"key\"", jsonProvider.getNodeType(entry));

			JsonNode key = jsonProvider.get(entry, "key");
			if (key == null)
				key = jsonProvider.get(entry, "Key");
			if (key == null)
				key = jsonProvider.get(entry, "name");
			if (key == null)
				key = jsonProvider.get(entry, "Name");
			if (key == null || jsonProvider.getNodeType(key) != JsonNodeType.STRING)
				throw new JsonQueryTypeException(jsonProvider, "Cannot use %s as object key", key == null ? jsonProvider.createNull() : key);

			JsonNode value = jsonProvider.get(entry, "value");
			if (value == null)
				value = jsonProvider.get(entry, "Value");

			jsonProvider.set(out, jsonProvider.asText(key), value == null ? jsonProvider.createNull() : value);
		}

		output.emit(out, null);
	}
}
