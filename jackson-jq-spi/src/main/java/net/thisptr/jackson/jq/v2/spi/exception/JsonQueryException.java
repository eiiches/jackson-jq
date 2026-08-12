package net.thisptr.jackson.jq.v2.spi.exception;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class JsonQueryException extends RuntimeException {
    private static final long serialVersionUID = -7241258446595502920L;

    public JsonQueryException(String msg) {
        super(msg);
    }

    public JsonQueryException(Throwable e) {
        super(e);
    }

    public JsonQueryException(String msg, Throwable rootCause) {
        super(msg, rootCause);
    }

    /**
     * Simple format constructor without JsonProvider - uses default Object.toString() for arguments.
     */
    public JsonQueryException(String format, Object... args) {
        this(String.format(format, args));
    }

    public <JsonNode> JsonNode getMessageAsJsonNode(JsonProvider<JsonNode> jsonProvider) {
        return jsonProvider.createString(getMessage());
    }

    public JsonQueryException(JsonProvider<?> jsonProvider, String format, Object... args) {
        this(format(jsonProvider, format, args));
    }

    private static final int MAX_JSON_STRING_LENGTH = 14;

    private static <JsonNode> String format(JsonProvider<JsonNode> jsonProvider, String format, Object... args) {
        Object[] formattedArguments = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            if (jsonProvider.isJsonNodeInstance(args[i])) {
                @SuppressWarnings("unchecked") JsonNode node = (JsonNode) args[i];
                @Var String json;
                try {
                    json = truncate(jsonProvider.toString(node), MAX_JSON_STRING_LENGTH);
                } catch (Exception e) {
                    json = "<failed to format json>";
                }
                formattedArguments[i] = String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(), json);
            } else if (args[i] instanceof JsonNodeType) {
                JsonNodeType type = (JsonNodeType) args[i];
                formattedArguments[i] = type.toString().toLowerCase();
            } else {
                formattedArguments[i] = args[i];
            }
        }
        return String.format(format, formattedArguments);
    }

    private static String truncate(String text, int length) {
        if (text.length() <= length)
            return text;
        return text.substring(0, length - 3) + "...";
    }
}
