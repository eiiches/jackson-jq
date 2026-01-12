package net.thisptr.jackson.jq.exception;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.misc.Strings;

public class JsonQueryException extends RuntimeException {
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

    /**
     * Simple format constructor without JsonProvider - uses default Object.toString() for arguments.
     */
    public JsonQueryException(final String format, final Object... args) {
        this(String.format(format, args));
    }

    public <JsonNode> JsonNode getMessageAsJsonNode(final JsonProvider<JsonNode> jsonProvider) {
        return jsonProvider.createString(getMessage());
    }

    public JsonQueryException(final JsonProvider<?> jsonProvider, final String format, final Object... args) {
        this(format(jsonProvider, format, args));
    }

    private static final int MAX_JSON_STRING_LENGTH = 14;

    private static <JsonNode> String format(final JsonProvider<JsonNode> jsonProvider, final String format, final Object... args) {
        final Object[] formattedArguments = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            if (jsonProvider.isJsonNodeInstance(args[i])) {
                @SuppressWarnings("unchecked") final JsonNode node = (JsonNode) args[i];
                String json;
                try {
                    json = Strings.truncate(jsonProvider.toString(node), MAX_JSON_STRING_LENGTH);
                } catch (Exception e) {
                    json = "<failed to format json>";
                }
                formattedArguments[i] = String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(), json);
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
