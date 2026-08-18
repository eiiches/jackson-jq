package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.v2.core.internal.misc.Strings;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "@sh", nargs = 0)
public class AtShFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (scope, in, ipath, output, ignoredRequirePath) -> {
			JsonNodeType type = jsonProvider.getNodeType(in);
			if (type == JsonNodeType.ARRAY) {
				List<String> tokens = new ArrayList<>();
				Iterator<JsonNode> iter = jsonProvider.elements(in);
				while (iter.hasNext()) {
					JsonNode i = iter.next();
					JsonNodeType iType = jsonProvider.getNodeType(i);
					if (iType == JsonNodeType.STRING) {
						tokens.add(escape(jsonProvider.asText(i)));
					} else if (isValueNode(iType)) {
						tokens.add(jsonProvider.toString(i));
					} else {
						throw new IllegalJsonInputException(iType + " cannot be escaped for shell");
					}
				}
				output.emit(jsonProvider.createString(Strings.join(" ", tokens)), null);
			} else if (type == JsonNodeType.STRING) {
				output.emit(jsonProvider.createString(escape(jsonProvider.asText(in))), null);
			} else if (isValueNode(type)) {
				output.emit(jsonProvider.createString(jsonProvider.toString(in)), null);
			} else {
				throw new IllegalJsonInputException(type + " cannot be escaped for shell");
			}
		};
	}

	private static boolean isValueNode(JsonNodeType type) {
		return type == JsonNodeType.STRING || type == JsonNodeType.NUMBER || type == JsonNodeType.BOOLEAN || type == JsonNodeType.NULL;
	}

	public String escape(String text) {
		StringBuilder builder = new StringBuilder("'");
		for (char ch : text.toCharArray()) {
			switch (ch) {
			case '\'':
				builder.append("'\\''");
				break;
			case '\0':
				builder.append("\\0");
				break;
			default:
				builder.append(ch);
				break;
			}
		}
		builder.append("'");
		return builder.toString();
	}
}
