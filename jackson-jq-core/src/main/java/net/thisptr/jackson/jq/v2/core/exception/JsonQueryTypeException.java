package net.thisptr.jackson.jq.v2.core.exception;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryTypeException(String msg) {
		super(msg);
	}

	/**
	 * Simple format constructor without JsonProvider - uses default Object.toString() for arguments.
	 */
	public JsonQueryTypeException(String format, Object... args) {
		super(String.format(format, args));
	}

	public JsonQueryTypeException(JsonProvider<?> jsonProvider, String format, Object... args) {
		super(jsonProvider, format, args);
	}

	public JsonQueryTypeException(JsonProvider<?> jsonProvider, @Nullable Version version, String format, Object... args) {
		super(jsonProvider, version, format, args);
	}
}
