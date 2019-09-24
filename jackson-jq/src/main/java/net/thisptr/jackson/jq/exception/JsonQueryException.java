package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class JsonQueryException extends JsonProcessingException {
	private static final long serialVersionUID = -7241258446595502920L;

	public JsonQueryException(final String msg) {
		super(msg);
	}

	public JsonQueryException(final Throwable e) {
		super(e);
	}

	public JsonQueryException(final String msg, final Throwable rootCause) {
		super(msg, rootCause);
	}

	public JsonNode getMessageAsJsonNode() {
		return new TextNode(getMessage());
	}

	public JsonQueryException(final String format, final Object... args) {
		this(format(format, args));
	}

	private static final int MAX_JSON_STRING_LENGTH = 14;

	private static String format(final String format, final Object... args) {
		final Object[] formattedArguments = new Object[args.length];
		for (int i = 0; i < args.length; ++i) {
			if (args[i] instanceof JsonNode) {
				final JsonNode node = (JsonNode) args[i];
				String json;
				try {
					json = Strings.truncate(JsonNodeUtils.toString(node), MAX_JSON_STRING_LENGTH);
				} catch (Exception e) {
					json = "<failed to format json>";
				}
				formattedArguments[i] = String.format("%s (%s)", node.getNodeType().toString().toLowerCase(), json);
			} else if (args[i] instanceof JsonNodeType) {
				final JsonNodeType type = (JsonNodeType) args[i];
				formattedArguments[i] = type.toString().toLowerCase();
			} else {
				formattedArguments[i] = args[i];
			}
		}
		return String.format(format, formattedArguments);
	}
}
