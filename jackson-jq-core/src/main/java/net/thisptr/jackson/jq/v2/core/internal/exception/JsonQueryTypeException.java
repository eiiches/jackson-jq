package net.thisptr.jackson.jq.v2.core.internal.exception;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryTypeException extends JsonQueryException {
	private static final long serialVersionUID = -2719442463094461632L;

	public JsonQueryTypeException(String msg) {
		super(msg);
	}

	/**
	 * Formats the message with {@link String#format}; render JSON nodes with
	 * {@link ExceptionMessages#describe} and node types with {@link ExceptionMessages#typeName} first.
	 */
	@FormatMethod
	public JsonQueryTypeException(@FormatString String format, Object... args) {
		super(String.format(format, args));
	}
}
