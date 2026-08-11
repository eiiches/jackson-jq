package net.thisptr.jackson.jq.v2.core.exception;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IllegalJsonInputException extends JsonQueryException {
	private static final long serialVersionUID = -3734135414103466554L;

	public IllegalJsonInputException(final String msg) {
		super(msg);
	}
}
