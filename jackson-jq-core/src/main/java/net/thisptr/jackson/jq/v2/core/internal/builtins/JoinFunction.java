package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "join", nargs = 1)
public class JoinFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (sep, opath) -> {
				JsonNodeType inType = jsonProvider.getNodeType(in);
				if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
					throw new JsonQueryTypeException("Cannot iterate over %s", ExceptionMessages.describe(jsonProvider, version, in));

				@Var boolean first = true;
				StringBuilder builder = new StringBuilder();
				Iterator<JsonNode> iter = inType == JsonNodeType.ARRAY
						? jsonProvider.getArrayElements(in)
						: jsonProvider.getObjectMemberValues(in);
				while (iter.hasNext()) {
					JsonNode item = iter.next();
					if (!first) {
						JsonNodeType isepType = jsonProvider.getNodeType(sep);
						if (isepType == JsonNodeType.STRING) {
							builder.append(jsonProvider.getString(sep));
						} else if (isepType == JsonNodeType.NULL) {
							// append nothing
						} else {
							throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, jsonProvider.createString(builder.toString())), ExceptionMessages.describe(jsonProvider, version, sep));
						}
					}

					JsonNodeType itemType = jsonProvider.getNodeType(item);
					if (itemType == JsonNodeType.STRING) {
						builder.append(jsonProvider.getString(item));
					} else if (itemType == JsonNodeType.NULL) {
						// append nothing
					} else if (version.compareTo(Versions.JQ_1_6) >= 0 && (itemType == JsonNodeType.NUMBER || itemType == JsonNodeType.BOOLEAN)) {
						builder.append(jsonProvider.format(item));
					} else {
						if (version.compareTo(Versions.JQ_1_6) >= 0)
							throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, jsonProvider.createString(builder.toString())), ExceptionMessages.describe(jsonProvider, version, item));
						throw new JsonQueryTypeException("%s and %s cannot be added", ExceptionMessages.describe(jsonProvider, version, sep), ExceptionMessages.describe(jsonProvider, version, item));
					}

					first = false;
				}
				output.emit(jsonProvider.createString(builder.toString()), UntrackedPath.getInstance());
			});
		});
	}
}
