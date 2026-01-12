package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
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
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("@sh/0")
public class AtShFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.ARRAY) {
			final List<String> tokens = new ArrayList<>();
			final Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				final JsonNode i = iter.next();
				final JsonNodeType iType = jsonProvider.getNodeType(i);
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
	}

	private static boolean isValueNode(final JsonNodeType type) {
		return type == JsonNodeType.STRING || type == JsonNodeType.NUMBER || type == JsonNodeType.BOOLEAN || type == JsonNodeType.NULL;
	}

	public String escape(final String text) {
		final StringBuilder builder = new StringBuilder("'");
		for (final char ch : text.toCharArray()) {
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
