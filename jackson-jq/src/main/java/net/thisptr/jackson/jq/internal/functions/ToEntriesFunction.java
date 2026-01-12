package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
@BuiltinFunction("to_entries/0")
public class ToEntriesFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNode out = jsonProvider.createArray();
		final JsonNodeType inType = jsonProvider.getNodeType(in);

		if (inType == JsonNodeType.OBJECT) {
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(in);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> entry = iter.next();
				final JsonNode entryNode = jsonProvider.createObject();
				jsonProvider.set(entryNode, "key", jsonProvider.createString(entry.getKey()));
				jsonProvider.set(entryNode, "value", entry.getValue());
				jsonProvider.add(out, entryNode);
			}
		} else if (inType == JsonNodeType.ARRAY) {
			final Iterator<JsonNode> iter = jsonProvider.elements(in);
			for (int i = 0; iter.hasNext(); ++i) {
				final JsonNode value = iter.next();
				final JsonNode entryNode = jsonProvider.createObject();
				jsonProvider.set(entryNode, "key", jsonProvider.createInt(i));
				jsonProvider.set(entryNode, "value", value);
				jsonProvider.add(out, entryNode);
			}
		} else {
			throw new JsonQueryTypeException(jsonProvider, "%s has no keys", in);
		}

		output.emit(out, null);
	}
}
