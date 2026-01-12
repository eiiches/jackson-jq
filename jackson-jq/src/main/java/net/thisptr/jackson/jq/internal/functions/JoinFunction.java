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
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("join/1")
public class JoinFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (sep) -> {
			final JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", in);

			JsonNode isep = null;
			final StringBuilder builder = new StringBuilder();
			final Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				final JsonNode item = iter.next();
				if (isep != null) {
					final JsonNodeType isepType = jsonProvider.getNodeType(isep);
					if (isepType == JsonNodeType.STRING) {
						builder.append(jsonProvider.asText(isep));
					} else if (isepType == JsonNodeType.NULL) {
						// append nothing
					} else {
						throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be added", jsonProvider.createString(builder.toString()), isep);
					}
				}

				final JsonNodeType itemType = jsonProvider.getNodeType(item);
				if (itemType == JsonNodeType.STRING) {
					builder.append(jsonProvider.asText(item));
				} else if (itemType == JsonNodeType.NULL) {
					// append nothing
				} else if (version.compareTo(Versions.JQ_1_6) >= 0 && (itemType == JsonNodeType.NUMBER || itemType == JsonNodeType.BOOLEAN)) {
					// https://github.com/stedolan/jq/commit/e17ccf229723d776c0d49341665256b855c70bda
					// https://github.com/stedolan/jq/issues/930
					builder.append(jsonProvider.toString(item));
				} else {
					if (version.compareTo(Versions.JQ_1_6) >= 0)
						throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be added", jsonProvider.createString(builder.toString()), item);
					throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be added", sep, item);
				}

				isep = sep;
			}
			output.emit(jsonProvider.createString(builder.toString()), null);
		});
	}
}
