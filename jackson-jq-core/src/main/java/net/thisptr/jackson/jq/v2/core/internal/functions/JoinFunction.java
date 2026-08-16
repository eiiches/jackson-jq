package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "join", nargs = 1)
public class JoinFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, (sep) -> {
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
		};
	}
}
