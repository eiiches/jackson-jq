package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "from_entries", nargs = 0)
public class FromEntriesFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType != JsonNodeType.ARRAY && inType != JsonNodeType.OBJECT)
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot iterate over %s", in);

			Map<String, JsonNode> result = new LinkedHashMap<>();
			Iterator<JsonNode> iter = inType == JsonNodeType.ARRAY
					? jsonProvider.getArrayElements(in)
					: jsonProvider.getObjectMemberValues(in);
			while (iter.hasNext()) {
				JsonNode entry = iter.next();
				if (!jsonProvider.isObject(entry))
					throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, entry, jsonProvider.createString("key")));

				@Var JsonNode key = jsonProvider.getObjectMember(entry, "key");
				if (key == null)
					key = jsonProvider.getObjectMember(entry, "Key");
				if (key == null)
					key = jsonProvider.getObjectMember(entry, "name");
				if (key == null)
					key = jsonProvider.getObjectMember(entry, "Name");
				if (key == null || !jsonProvider.isString(key))
					throw new JsonQueryTypeException(jsonProvider, version, "Cannot use %s as object key", key == null ? jsonProvider.createNull() : key);

				@Var JsonNode value = jsonProvider.getObjectMember(entry, "value");
				if (value == null)
					value = jsonProvider.getObjectMember(entry, "Value");

				result.put(jsonProvider.getString(key), value == null ? jsonProvider.createNull() : value);
			}

			output.emit(jsonProvider.createObject(result), UntrackedPath.getInstance());
		});
	}
}
