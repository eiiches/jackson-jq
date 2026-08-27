package net.thisptr.jackson.jq.v2.core.exception;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
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
	@FormatMethod
	public JsonQueryTypeException(@FormatString String format, Object... args) {
		super(String.format(format, args));
	}

	@FormatMethod
	public JsonQueryTypeException(JsonProvider<?> jsonProvider, @Nullable Version version, @FormatString String format, Object... args) {
		super(ExceptionMessages.format(jsonProvider, version, format, args));
	}
}
