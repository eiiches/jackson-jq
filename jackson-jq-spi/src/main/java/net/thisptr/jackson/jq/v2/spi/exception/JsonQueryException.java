package net.thisptr.jackson.jq.v2.spi.exception;

import java.util.Locale;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;

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
		@Nullable String message = getMessage();
		return message == null ? jsonProvider.createNull() : jsonProvider.createString(message);
    }

    public JsonQueryException(JsonProvider<?> jsonProvider, String format, Object... args) {
        this(format(jsonProvider, null, format, args));
    }

    public JsonQueryException(JsonProvider<?> jsonProvider, @Nullable Version version, String format, Object... args) {
        this(format(jsonProvider, version, format, args));
    }

    private static <JsonNode> String format(JsonProvider<JsonNode> jsonProvider, @Nullable Version version, String format, Object... args) {
        Object[] formattedArguments = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            if (jsonProvider.isJsonNodeInstance(args[i])) {
                @SuppressWarnings("unchecked") JsonNode node = (JsonNode) args[i];
                @Var String json;
                try {
                    json = truncate(jsonProvider.toString(node), version);
                } catch (Exception e) {
                    json = "<failed to format json>";
                }
                formattedArguments[i] = String.format("%s (%s)", jsonProvider.getNodeType(node).toString().toLowerCase(Locale.ROOT), json);
            } else if (args[i] instanceof JsonNodeType) {
                JsonNodeType type = (JsonNodeType) args[i];
                formattedArguments[i] = type.toString().toLowerCase(Locale.ROOT);
            } else {
                formattedArguments[i] = args[i];
            }
        }
        return String.format(format, formattedArguments);
    }

    public static String truncate(String text, @Nullable Version version) {
        if (version != null && version.compareTo(Version.valueOf(1, 8, 2)) >= 0) {
            if (text.length() <= 29)
                return text;
            @Var char delim = 0;
            if (text.startsWith("\"")) delim = '"';
            else if (text.startsWith("[")) delim = ']';
            else if (text.startsWith("{")) delim = '}';
            int l = delim != 0 ? 25 : 26;
            return text.substring(0, l) + "..." + (delim != 0 ? delim : "");
        } else {
            if (text.length() <= 14)
                return text;
            return text.substring(0, 11) + "...";
        }
    }
}
