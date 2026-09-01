package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "to_entries", nargs = 0)
public class ToEntriesFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {

			List<JsonNode> result = new ArrayList<>();
			JsonNodeType inType = jsonProvider.getNodeType(in);

			if (inType == JsonNodeType.OBJECT) {
				Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.getObjectMembers(in);
				while (iter.hasNext()) {
					Map.Entry<String, JsonNode> entry = iter.next();
					Map<String, JsonNode> fields = new LinkedHashMap<>();
					fields.put("key", jsonProvider.createString(entry.getKey()));
					fields.put("value", entry.getValue());
					result.add(jsonProvider.createObject(fields));
				}
			} else if (inType == JsonNodeType.ARRAY) {
				Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
				for (int i = 0; iter.hasNext(); ++i) {
					JsonNode value = iter.next();
					Map<String, JsonNode> fields = new LinkedHashMap<>();
					fields.put("key", jsonProvider.createNumber(i));
					fields.put("value", value);
					result.add(jsonProvider.createObject(fields));
				}
			} else {
				throw new JsonQueryTypeException(jsonProvider, version, "%s has no keys", in);
			}

			output.emit(jsonProvider.createArray(result), UntrackedPath.getInstance());
		});
	}
}
