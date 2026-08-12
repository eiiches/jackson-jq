package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
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
@FunctionRegistration("join/1")
public class JoinFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (sep) -> {
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException(jsonProvider, "Cannot iterate over %s", in);

			@Var JsonNode isep = null;
			StringBuilder builder = new StringBuilder();
			Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				JsonNode item = iter.next();
				if (isep != null) {
					JsonNodeType isepType = jsonProvider.getNodeType(isep);
					if (isepType == JsonNodeType.STRING) {
						builder.append(jsonProvider.asText(isep));
					} else if (isepType == JsonNodeType.NULL) {
						// append nothing
					} else {
						throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot be added", jsonProvider.createString(builder.toString()), isep);
					}
				}

				JsonNodeType itemType = jsonProvider.getNodeType(item);
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
