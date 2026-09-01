package net.thisptr.jackson.jq.v2.core.internal.exception;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IllegalJsonInputException extends JsonQueryException {
	private static final long serialVersionUID = -3734135414103466554L;

	public IllegalJsonInputException(String msg) {
		super(msg);
	}
}
